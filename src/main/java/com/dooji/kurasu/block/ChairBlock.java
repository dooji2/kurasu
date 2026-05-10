package com.dooji.kurasu.block;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import com.dooji.kurasu.block.entity.AccessoryBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChairBlock extends BaseEntityBlock {
	public static final MapCodec<ChairBlock> CODEC = simpleCodec(ChairBlock::new);
	private static final VoxelShape NORTH_SHAPE = Block.box(2.0, 0.0, 0.0, 14.0, 9.0, 10.0);
	private static final VoxelShape SOUTH_SHAPE = Block.box(2.0, 0.0, 6.0, 14.0, 9.0, 16.0);
	private static final VoxelShape WEST_SHAPE = Block.box(0.0, 0.0, 2.0, 10.0, 9.0, 14.0);
	private static final VoxelShape EAST_SHAPE = Block.box(6.0, 0.0, 2.0, 16.0, 9.0, 14.0);

	public ChairBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<ChairBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getStaticShape(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getStaticShape(state);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AccessoryBlockEntity(KurasuBlockEntityTypes.CHAIR, pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player.isShiftKeyDown()) {
			return tryToggleOperatorLock(level, pos, player);
		}

		return trySit(state, level, pos, player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!itemStack.isEmpty()) {
			return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
		}

		return trySit(state, level, pos, player);
	}

	@Override
	public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
		super.destroy(level, pos, state);

		if (level instanceof Level actualLevel && !actualLevel.isClientSide()) {
			removeChairSeat(actualLevel, pos);
		}
	}

	private VoxelShape getStaticShape(BlockState state) {
		return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			case EAST -> EAST_SHAPE;
			default -> NORTH_SHAPE;
		};
	}

	private InteractionResult trySit(BlockState state, Level level, BlockPos pos, Player player) {
		if (player.isShiftKeyDown() || player.isPassenger() || player.isSpectator()) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		AreaEffectCloud existingSeat = findChairSeat(level, pos);

		if (existingSeat != null) {
			if (existingSeat.getFirstPassenger() != null) {
				return InteractionResult.PASS;
			}

			existingSeat.discard();
		}

		AreaEffectCloud seat = createChairSeat(level, pos, state);

		if (!player.startRiding(seat)) {
			seat.discard();
			return InteractionResult.PASS;
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	private InteractionResult tryToggleOperatorLock(Level level, BlockPos pos, Player player) {
		if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (level.getBlockEntity(pos) instanceof AccessoryBlockEntity blockEntity) {
			blockEntity.setOperatorLocked(!blockEntity.isOperatorLocked());
			player.sendOverlayMessage(Component.translatable(blockEntity.isOperatorLocked() ? "message.kurasu.op_locked" : "message.kurasu.op_unlocked"));
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	private AreaEffectCloud createChairSeat(Level level, BlockPos pos, BlockState state) {
		BlockPos anchorPos = pos.immutable();
		Block chairBlock = state.getBlock();
		Vec3 seatPos = getSeatPosition(state, pos);
		AreaEffectCloud seat = new AreaEffectCloud(level, seatPos.x(), seatPos.y(), seatPos.z()) {
			@Override
			public void tick() {
				if (getFirstPassenger() == null || !level().getBlockState(anchorPos).is(chairBlock)) {
					discard();
					return;
				}

				super.tick();
			}
		};

		seat.setNoGravity(true);
		seat.setInvisible(true);
		seat.setWaitTime(0);
		seat.setRadius(0.0f);
		seat.setDuration(Integer.MAX_VALUE);
		level.addFreshEntity(seat);
		return seat;
	}

	private AreaEffectCloud findChairSeat(Level level, BlockPos pos) {
		return level.getEntitiesOfClass(AreaEffectCloud.class, new AABB(pos).inflate(0.5), cloud -> cloud.isInvisible() && cloud.isNoGravity() && cloud.getRadius() == 0.0f)
			.stream()
			.findFirst()
			.orElse(null);
	}

	private void removeChairSeat(Level level, BlockPos pos) {
		AreaEffectCloud seat = findChairSeat(level, pos);

		if (seat != null) {
			if (level instanceof ServerLevel serverLevel) {
				seat.kill(serverLevel);
				return;
			}

			seat.discard();
		}
	}

	private Vec3 getSeatPosition(BlockState state, BlockPos pos) {
		Vec3 center = Vec3.atBottomCenterOf(pos);

		return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
			case SOUTH -> center.add(0.0, 0.0, 0.1875);
			case WEST -> center.add(-0.1875, 0.0, 0.0);
			case EAST -> center.add(0.1875, 0.0, 0.0);
			default -> center.add(0.0, 0.0, -0.1875);
		};
	}
}

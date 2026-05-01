package com.dooji.kurasu.block;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.block.entity.LockerBlockEntity;
import com.dooji.kurasu.block.entity.SafeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SafeBlock extends BaseEntityBlock {
	public static final MapCodec<SafeBlock> CODEC = simpleCodec(SafeBlock::new);
	private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 16.0);
	private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 9.0);
	private static final VoxelShape WEST_SHAPE = Block.box(7.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 0.0, 9.0, 16.0, 16.0);

	public SafeBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
	}

	@Override
	public MapCodec<SafeBlock> codec() {
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
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SafeBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SafeBlockEntity blockEntity) {
			if (blockEntity.isStructureOpen()) {
				blockEntity.setStructureState(false, true, "", false);
				player.sendOverlayMessage(Component.translatable("message.kurasu.safe_locked"));
				return InteractionResult.SUCCESS_SERVER;
			}

			if (blockEntity.isStructureLocked() && !blockEntity.isStructureOpen()) {
				player.sendOverlayMessage(Component.translatable("message.kurasu.safe_locked"));
				return InteractionResult.SUCCESS_SERVER;
			}

			blockEntity.setStructureState(true, false, "", false);
		}

		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (KurasuItems.getAccessoryId(itemStack) != null) {
			return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
		}

		return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, KurasuBlockEntityTypes.SAFE, LockerBlockEntity::tick);
	}

	private VoxelShape getStaticShape(BlockState state) {
		return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			case EAST -> EAST_SHAPE;
			default -> NORTH_SHAPE;
		};
	}
}

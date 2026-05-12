package com.dooji.kurasu.block;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.block.entity.LockerBlockEntity;
import com.dooji.kurasu.item.KeyItem;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LockerBlock extends BaseEntityBlock {
	public static final MapCodec<LockerBlock> CODEC = simpleCodec(LockerBlock::new);
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	private static final int SHOVE_SELECT_TICKS = 100;
	private static final double SHOVE_SELECT_RANGE = 4.0;
	private static final String LOCKER_SEAT_TAG = "kurasu_locker_seat";
	private static final Map<UUID, PendingShove> PENDING_SHOVES = new HashMap<>();
	private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 16.0);
	private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 9.0);
	private static final VoxelShape WEST_SHAPE = Block.box(7.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 0.0, 9.0, 16.0, 16.0);

	public LockerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(PART, Part.SINGLE));
	}

	@Override
	public MapCodec<LockerBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING, PART);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockState belowState = level.getBlockState(pos.below());
		Direction belowFacing = belowState.is(this) ? belowState.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
		BlockState aboveState = level.getBlockState(pos.above());
		Direction aboveFacing = aboveState.is(this) ? aboveState.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;

		if (belowFacing != null && aboveFacing != null && belowFacing != aboveFacing) {
			return null;
		}

		if (belowFacing != null) {
			facing = belowFacing;
		} else if (aboveFacing != null) {
			facing = aboveFacing;
		}

		return getPartState(level, pos, defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(PART, Part.SINGLE));
	}

	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState updatedState = direction.getAxis() == Direction.Axis.Y ? getPartState(level, pos, state) : state;
		return super.updateShape(updatedState, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);

		if (!level.isClientSide() && !oldState.is(this)) {
			refreshStack(level, pos);
		}
	}

	@Override
	public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
		super.destroy(level, pos, state);

		if (level instanceof Level actualLevel && !actualLevel.isClientSide()) {
			removeLockerSeat(actualLevel, findControllerPos(actualLevel, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
			refreshStack(actualLevel, pos.below());
			refreshStack(actualLevel, pos.above());
		}
	}

	@Override
	public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
		BlockState state = level.getBlockState(pos);
		BlockPos controllerPos = state.is(this) ? findControllerPos(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING)) : pos;
		super.wasExploded(level, pos, explosion);

		removeLockerSeat(level, controllerPos);
		refreshStack(level, pos.below());
		refreshStack(level, pos.above());
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
		return new LockerBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			refreshStack(level, pos);

			if (level.getBlockEntity(pos) instanceof LockerBlockEntity blockEntity) {
				if (player.isShiftKeyDown()) {
					if (!canUseProtectedFeatures(player, blockEntity)) {
						return InteractionResult.SUCCESS_SERVER;
					}

					if (blockEntity.isStructureLocked() && !blockEntity.isStructureOpen()) {
						player.sendOverlayMessage(Component.translatable("message.kurasu.locker_locked"));
						return InteractionResult.SUCCESS_SERVER;
					}

					if (!blockEntity.isStructureOpen()) {
						blockEntity.setStructureOpen(true);
					}

					if (armPlayerShove(state, level, pos, player)) {
						player.sendOverlayMessage(Component.translatable("message.kurasu.locker_shove_ready"));
					} else {
						player.sendOverlayMessage(Component.translatable("message.kurasu.locker_shove_cancelled"));
					}
					return InteractionResult.SUCCESS_SERVER;
				}

				if (blockEntity.isStructureLocked() && !blockEntity.isStructureOpen()) {
					player.sendOverlayMessage(Component.translatable("message.kurasu.locker_locked"));
				} else {
					blockEntity.setStructureOpen(!blockEntity.isStructureOpen());
				}
			}
		}

		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	public static InteractionResult tryShoveSelectedPlayer(Player player, Entity target) {
		if (player.level().isClientSide() || !(player instanceof ServerPlayer) || !(target instanceof Player targetPlayer)) {
			return InteractionResult.PASS;
		}

		PendingShove pendingShove = PENDING_SHOVES.remove(player.getUUID());

		if (pendingShove == null || pendingShove.expired(player.level().getGameTime())) {
			return InteractionResult.PASS;
		}

		if (targetPlayer.isSpectator() || targetPlayer.isPassenger() || player.distanceTo(targetPlayer) > SHOVE_SELECT_RANGE) {
			return InteractionResult.PASS;
		}

		Level level = player.level();
		BlockState state = level.getBlockState(pendingShove.controllerPos());

		if (!(state.getBlock() instanceof LockerBlock locker) || state.getValue(BlockStateProperties.HORIZONTAL_FACING) != pendingShove.facing()) {
			return InteractionResult.PASS;
		}

		if (!(level.getBlockEntity(pendingShove.controllerPos()) instanceof LockerBlockEntity blockEntity) || !blockEntity.isStructureOpen()) {
			return InteractionResult.PASS;
		}

		if (blockEntity.isOperatorLocked() && !isOperator(player)) {
			return InteractionResult.PASS;
		}

		return locker.shovePlayerIntoLocker(level, pendingShove.controllerPos(), state, blockEntity, targetPlayer);
	}

	public static boolean isLockerSeat(Entity entity) {
		return entity instanceof AreaEffectCloud cloud && cloud.entityTags().contains(LOCKER_SEAT_TAG);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (itemStack.getItem() == this.asItem()) {
			return InteractionResult.PASS;
		}

		if (itemStack.getItem() == KurasuItems.OP_TOOL) {
			return useOperatorTool(level, pos, player);
		}

		if (itemStack.getItem() == KurasuItems.KEY) {
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LockerBlockEntity blockEntity) {
				if (!canUseProtectedFeatures(player, blockEntity)) {
					return InteractionResult.SUCCESS_SERVER;
				}

				useKeyOnLocker(player, itemStack, blockEntity);
			}

			return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
		}

		if (KurasuItems.getAccessoryId(itemStack) != null) {
			return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
		}

		return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
	}

	private InteractionResult useOperatorTool(Level level, BlockPos pos, Player player) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		refreshStack(level, pos);

		if (!isOperator(player)) {
			player.sendOverlayMessage(Component.translatable("message.kurasu.op_only"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (level.getBlockEntity(pos) instanceof LockerBlockEntity blockEntity) {
			blockEntity.setOperatorLocked(!blockEntity.isOperatorLocked());
			player.sendOverlayMessage(Component.translatable(blockEntity.isOperatorLocked() ? "message.kurasu.op_locked" : "message.kurasu.op_unlocked"));
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, KurasuBlockEntityTypes.LOCKER, LockerBlockEntity::tick);
	}

	private BlockState getPartState(LevelReader level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		int index = 0;

		for (BlockPos scanPos = pos.below(); matchesStackNeighbor(level.getBlockState(scanPos), facing); scanPos = scanPos.below()) {
			index++;
		}

		int height = index + 1;

		for (BlockPos scanPos = pos.above(); matchesStackNeighbor(level.getBlockState(scanPos), facing); scanPos = scanPos.above()) {
			height++;
		}

		return state.setValue(PART, getPart(index, height));
	}

	private void refreshStack(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);

		if (!state.is(this)) {
			return;
		}

		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		BlockPos controllerPos = findControllerPos(level, pos, facing);
		boolean open = false;
		boolean locked = false;
		boolean pickedOpen = false;
		boolean operatorLocked = false;
		String lockId = "";
		List<BlockPos> stack = collectStack(level, controllerPos, facing);

		for (BlockPos scanPos : stack) {
			if (level.getBlockEntity(scanPos) instanceof LockerBlockEntity blockEntity) {
				open |= blockEntity.isStructureOpen();
				locked |= blockEntity.isStructureLocked();
				pickedOpen |= blockEntity.isStructurePickedOpen();
				operatorLocked |= blockEntity.isOperatorLocked();

				if (lockId.isBlank() && !blockEntity.getStructureLockId().isBlank()) {
					lockId = blockEntity.getStructureLockId();
				}
			}
		}

		for (int i = 0; i < stack.size(); i++) {
			BlockPos scanPos = stack.get(i);
			BlockState scanState = level.getBlockState(scanPos);
			Part part = getPart(i, stack.size());

			if (scanState.getValue(PART) != part) {
				level.setBlock(scanPos, scanState.setValue(PART, part), Block.UPDATE_CLIENTS);
			}

			if (level.getBlockEntity(scanPos) instanceof LockerBlockEntity blockEntity) {
				blockEntity.setStructureData(controllerPos, open, locked, lockId, open && pickedOpen, operatorLocked);
			}
		}
	}

	private BlockPos findControllerPos(Level level, BlockPos pos, Direction facing) {
		BlockPos controllerPos = pos.immutable();

		while (matchesStackNeighbor(level.getBlockState(controllerPos.below()), facing)) {
			controllerPos = controllerPos.below().immutable();
		}

		return controllerPos;
	}

	private List<BlockPos> collectStack(Level level, BlockPos controllerPos, Direction facing) {
		List<BlockPos> stack = new ArrayList<>();

		for (BlockPos scanPos = controllerPos; matchesStackNeighbor(level.getBlockState(scanPos), facing); scanPos = scanPos.above()) {
			stack.add(scanPos.immutable());
		}

		return stack;
	}

	private void useKeyOnLocker(Player player, ItemStack keyStack, LockerBlockEntity blockEntity) {
		String currentLockId = blockEntity.getStructureLockId();
		String keyLockId = KeyItem.getLockId(keyStack);

		if (currentLockId.isBlank()) {
			String newLockId = keyLockId.isBlank() ? UUID.randomUUID().toString() : keyLockId;
			KeyItem.setLockId(keyStack, newLockId);
			blockEntity.setStructureState(false, true, newLockId, false);
			player.sendOverlayMessage(Component.translatable("message.kurasu.locker_locked"));
			return;
		}

		if (keyLockId.isBlank()) {
			player.sendOverlayMessage(Component.translatable("message.kurasu.blank_key"));
			return;
		}

		if (!currentLockId.equals(keyLockId)) {
			player.sendOverlayMessage(Component.translatable("message.kurasu.wrong_key"));
			return;
		}

		if (blockEntity.isStructureLocked()) {
			blockEntity.setStructureState(true, false, currentLockId, false);
			player.sendOverlayMessage(Component.translatable("message.kurasu.locker_unlocked"));
			return;
		}

		blockEntity.setStructureState(false, true, currentLockId, false);
		player.sendOverlayMessage(Component.translatable("message.kurasu.locker_locked"));
	}

	private boolean canUseProtectedFeatures(Player player, LockerBlockEntity blockEntity) {
		if (!blockEntity.isOperatorLocked() || isOperator(player)) {
			return true;
		}

		player.sendOverlayMessage(Component.translatable("message.kurasu.op_only"));
		return false;
	}

	private static boolean isOperator(Player player) {
		MinecraftServer server = player.level().getServer();
		return server != null && (server.getPlayerList().isOp(player.nameAndId()) || server.isSingleplayerOwner(player.nameAndId()));
	}

	private boolean armPlayerShove(BlockState state, Level level, BlockPos pos, Player player) {
		BlockPos controllerPos = findControllerPos(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
		PendingShove pendingShove = PENDING_SHOVES.get(player.getUUID());

		if (pendingShove != null && pendingShove.controllerPos().equals(controllerPos) && !pendingShove.expired(level.getGameTime())) {
			PENDING_SHOVES.remove(player.getUUID());
			return false;
		}

		PENDING_SHOVES.put(player.getUUID(), new PendingShove(controllerPos, state.getValue(BlockStateProperties.HORIZONTAL_FACING), level.getGameTime() + SHOVE_SELECT_TICKS));
		return true;
	}

	private InteractionResult shovePlayerIntoLocker(Level level, BlockPos controllerPos, BlockState controllerState, LockerBlockEntity blockEntity, Player target) {
		if (target.isPassenger() || target.isSpectator()) {
			return InteractionResult.PASS;
		}

		AreaEffectCloud existingSeat = findLockerSeat(level, controllerPos);

		if (existingSeat != null) {
			if (existingSeat.getFirstPassenger() != null) {
				return InteractionResult.PASS;
			}

			existingSeat.discard();
		}

		AreaEffectCloud seat = createLockerSeat(level, controllerPos, controllerState);

		if (!target.startRiding(seat)) {
			seat.discard();
			return InteractionResult.PASS;
		}

		giveLockpickIfNeeded(target);
		blockEntity.setStructureOpen(false);
		return InteractionResult.SUCCESS_SERVER;
	}

	private void giveLockpickIfNeeded(Player player) {
		if (player.getInventory().contains(stack -> stack.getItem() == KurasuItems.LOCKPICK)) {
			return;
		}

		ItemStack lockpick = new ItemStack(KurasuItems.LOCKPICK);

		if (!player.getInventory().add(lockpick)) {
			player.drop(lockpick, false);
		}
	}

	private AreaEffectCloud createLockerSeat(Level level, BlockPos pos, BlockState state) {
		BlockPos anchorPos = pos.immutable();
		Block lockerBlock = state.getBlock();
		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		Vec3 seatPos = Vec3.atBottomCenterOf(pos).add(facing.getStepX() * -0.1875, 0.0, facing.getStepZ() * -0.1875);
		AreaEffectCloud seat = new AreaEffectCloud(level, seatPos.x(), seatPos.y(), seatPos.z()) {
			@Override
			public void tick() {
				if (getFirstPassenger() == null || !level().getBlockState(anchorPos).is(lockerBlock)) {
					discard();
					return;
				}

				if (level().getBlockEntity(anchorPos) instanceof LockerBlockEntity blockEntity && blockEntity.isStructureOpen()) {
					discard();
					return;
				}

				super.tick();
			}

			@Override
			protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
				return new Vec3(0.0, 0.0, 0.0);
			}
		};

		seat.setNoGravity(true);
		seat.setInvisible(true);
		seat.setWaitTime(0);
		seat.setRadius(0.0f);
		seat.setDuration(Integer.MAX_VALUE);
		seat.addTag(LOCKER_SEAT_TAG);
		level.addFreshEntity(seat);
		return seat;
	}

	private AreaEffectCloud findLockerSeat(Level level, BlockPos pos) {
		return level.getEntitiesOfClass(AreaEffectCloud.class, new AABB(pos).inflate(0.5), LockerBlock::isLockerSeat)
			.stream()
			.findFirst()
			.orElse(null);
	}

	private void removeLockerSeat(Level level, BlockPos pos) {
		AreaEffectCloud seat = findLockerSeat(level, pos);

		if (seat == null) {
			return;
		}

		if (level instanceof ServerLevel serverLevel) {
			seat.kill(serverLevel);
			return;
		}

		seat.discard();
	}

	private Part getPart(int index, int height) {
		if (height <= 1) {
			return Part.SINGLE;
		}

		if (index == 0) {
			return Part.BOTTOM;
		}

		if (index == height - 1) {
			return Part.TOP;
		}

		int middleIndex = (height - 1) / 2;
		return index == middleIndex ? Part.MIDDLE : Part.MIDDLE_1;
	}

	private boolean matchesStackNeighbor(BlockState state, Direction facing) {
		return state.is(this) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
	}

	private record PendingShove(BlockPos controllerPos, Direction facing, long expiresAt) {
		private boolean expired(long gameTime) {
			return gameTime > this.expiresAt;
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

	public enum Part implements StringRepresentable {
		SINGLE("single"),
		BOTTOM("bottom"),
		MIDDLE("middle"),
		MIDDLE_1("middle_1"),
		TOP("top");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}

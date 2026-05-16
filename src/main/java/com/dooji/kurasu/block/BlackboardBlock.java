package com.dooji.kurasu.block;

import com.dooji.kurasu.KurasuItems;
import com.dooji.kurasu.KurasuPermissions;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlackboardBlock extends BaseEntityBlock {
	public static final MapCodec<BlackboardBlock> CODEC = simpleCodec(BlackboardBlock::new);
	public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 3);
	private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 13.5, 16.0, 16.0, 16.0);
	private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.5);
	private static final VoxelShape WEST_SHAPE = Block.box(13.5, 0.0, 0.0, 16.0, 16.0, 16.0);
	private static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 0.0, 2.5, 16.0, 16.0);

	public BlackboardBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
			.setValue(SECTION, 0));
	}

	@Override
	public MapCodec<BlackboardBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.DOUBLE_BLOCK_HALF, SECTION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos anchorPos = context.getClickedPos();
		Level level = context.getLevel();

		if (!canPlaceStructure(level, anchorPos, facing)) {
			return null;
		}

		return defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
			.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
			.setValue(SECTION, 0);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		Direction extension = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getCounterClockWise();

		for (int section = 0; section < 4; section++) {
			BlockPos lowerPos = pos.relative(extension, section);
			BlockState lowerState = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER).setValue(SECTION, section);
			BlockState upperState = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER).setValue(SECTION, section);
			level.setBlock(lowerPos, lowerState, Block.UPDATE_ALL);
			level.setBlock(lowerPos.above(), upperState, Block.UPDATE_ALL);
		}
	}

	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
		int section = state.getValue(SECTION);
		Direction extension = facing.getCounterClockWise();

		if (direction == half.getDirectionToOther()) {
			return matchesVerticalNeighbor(neighborState, facing, half, section) ? state : Blocks.AIR.defaultBlockState();
		}

		if (direction == extension) {
			return section == 3 || matchesHorizontalNeighbor(neighborState, facing, half, section + 1) ? state : Blocks.AIR.defaultBlockState();
		}

		if (direction == extension.getOpposite()) {
			return section == 0 || matchesHorizontalNeighbor(neighborState, facing, half, section - 1) ? state : Blocks.AIR.defaultBlockState();
		}

		return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
			BlockState belowState = level.getBlockState(pos.below());
			return matchesVerticalNeighbor(belowState, state.getValue(BlockStateProperties.HORIZONTAL_FACING), DoubleBlockHalf.UPPER, state.getValue(SECTION));
		}

		return super.canSurvive(state, level, pos);
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
		if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER || state.getValue(SECTION) != 0) {
			return null;
		}

		return new BlackboardBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (itemStack.getItem() != KurasuItems.OP_TOOL) {
			return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!KurasuPermissions.canSwitchGameModes(player)) {
			player.sendOverlayMessage(Component.translatable("message.kurasu.op_only"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (level.getBlockEntity(getAnchorPos(pos, state)) instanceof BlackboardBlockEntity blockEntity) {
			blockEntity.setOperatorLocked(!blockEntity.isOperatorLocked());
			player.sendOverlayMessage(Component.translatable(blockEntity.isOperatorLocked() ? "message.kurasu.op_locked" : "message.kurasu.op_unlocked"));
		}

		return InteractionResult.SUCCESS_SERVER;
	}

	public BlockPos getAnchorPos(BlockPos pos, BlockState state) {
		BlockPos anchorPos = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
		return anchorPos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise(), state.getValue(SECTION));
	}

	private boolean canPlaceStructure(Level level, BlockPos anchorPos, Direction facing) {
		if (anchorPos.getY() >= level.getMaxY()) {
			return false;
		}

		Direction extension = facing.getCounterClockWise();

		for (int section = 0; section < 4; section++) {
			BlockPos lowerPos = anchorPos.relative(extension, section);
			BlockPos upperPos = lowerPos.above();

			if (!level.getBlockState(lowerPos).canBeReplaced() || !level.getBlockState(upperPos).canBeReplaced()) {
				return false;
			}
		}

		return true;
	}

	private boolean matchesVerticalNeighbor(BlockState neighborState, Direction facing, DoubleBlockHalf currentHalf, int section) {
		return neighborState.is(this)
			&& neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing
			&& neighborState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != currentHalf
			&& neighborState.getValue(SECTION) == section;
	}

	private boolean matchesHorizontalNeighbor(BlockState neighborState, Direction facing, DoubleBlockHalf half, int expectedSection) {
		return neighborState.is(this)
			&& neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing
			&& neighborState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == half
			&& neighborState.getValue(SECTION) == expectedSection;
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

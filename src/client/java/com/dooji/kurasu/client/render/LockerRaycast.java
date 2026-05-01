package com.dooji.kurasu.client.render;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.block.entity.AccessoryBlockEntity;
import com.dooji.kurasu.block.entity.AccessoryBlockEntity.PlacedAccessory;
import com.dooji.kurasu.block.entity.LockerBlockEntity;
import com.dooji.kurasu.block.BlackboardBlock;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class LockerRaycast {
	public static Hit raycastPlayerView(Player player, float partialTick, AccessoryHitMode accessoryHitMode) {
		Vec3 start = player.getEyePosition(partialTick);
		Vec3 direction = player.getViewVector(partialTick);
		double reach = player.blockInteractionRange();

		Vec3 end = start.add(direction.scale(reach));
		double maxDistance = reach;
		BlockHitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

		if (blockHit.getType() != HitResult.Type.MISS && !isStorageHit(player.level(), blockHit)) {
			maxDistance = Math.min(maxDistance, start.distanceTo(blockHit.getLocation()));
		}

		return raycast(player.level(), start, end, maxDistance, accessoryHitMode);
	}

	private static boolean isStorageHit(Level level, BlockHitResult blockHit) {
		BlockPos blockPos = blockHit.getBlockPos();
		return ObjModels.getSurfaceMesh(level, blockPos, level.getBlockState(blockPos)) != null;
	}

	private static Hit raycast(Level level, Vec3 start, Vec3 end, double maxDistance, AccessoryHitMode accessoryHitMode) {
		Vec3 direction = end.subtract(start);
		double directionLength = direction.length();

		if (directionLength <= 0.000001) {
			return null;
		}

		Vec3 rayDirection = direction.scale(1.0 / directionLength);
		double hitLimit = Math.min(maxDistance, directionLength);
		int minX = (int) Math.floor(Math.min(start.x, end.x) - 1.0);
		int minY = (int) Math.floor(Math.min(start.y, end.y) - 1.0);
		int minZ = (int) Math.floor(Math.min(start.z, end.z) - 1.0);
		int maxX = (int) Math.floor(Math.max(start.x, end.x) + 1.0);
		int maxY = (int) Math.floor(Math.max(start.y, end.y) + 1.0);
		int maxZ = (int) Math.floor(Math.max(start.z, end.z) + 1.0);
		Hit bestHit = null;
		Set<BlockPos> testedPositions = new HashSet<>();

		for (BlockPos candidatePos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
			BlockState state = level.getBlockState(candidatePos);
			BlockPos blockEntityPos = candidatePos.immutable();

			if (state.getBlock() instanceof BlackboardBlock blackboard) {
				blockEntityPos = blackboard.getAnchorPos(blockEntityPos, state).immutable();
			}

			if (!testedPositions.add(blockEntityPos)) {
				continue;
			}

			if (!(level.getBlockEntity(blockEntityPos) instanceof AccessoryBlockEntity)) {
				continue;
			}

			Hit hit = raycastSurface(level, blockEntityPos, level.getBlockState(blockEntityPos), start, rayDirection, hitLimit, accessoryHitMode);

			if (hit != null && (bestHit == null || hit.distance < bestHit.distance)) {
				bestHit = hit;
				hitLimit = hit.distance;
			}
		}

		return bestHit;
	}

	private static Hit raycastSurface(Level level, BlockPos blockPos, BlockState state, Vec3 start, Vec3 rayDirection, double maxDistance, AccessoryHitMode accessoryHitMode) {
		ObjMesh mesh = ObjModels.getSurfaceMesh(level, blockPos, state);

		if (mesh == null) {
			return null;
		}

		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		float openProgress = 0.0f;
		AccessoryBlockEntity blockEntity = null;

		if (level.getBlockEntity(blockPos) instanceof AccessoryBlockEntity foundBlockEntity) {
			blockEntity = foundBlockEntity;

			if (foundBlockEntity instanceof LockerBlockEntity lockerBlockEntity) {
				openProgress = lockerBlockEntity.getOpenProgress(1.0f);
			}
		}

		float easedOpen = openProgress * openProgress * (3.0f - 2.0f * openProgress);
		float doorAngle = easedOpen * (float) Math.toRadians(105.0);
		Vector3f doorPivot = mesh.doorPivot;
		Hit bestHit = null;

		for (MeshLayer layer : mesh.layers) {
			for (MeshFace face : layer.faces) {
				Hit faceHit = raycastFace(blockPos, facing, face, start, rayDirection, maxDistance, doorPivot, doorAngle);

				if (faceHit != null && (bestHit == null || faceHit.distance < bestHit.distance)) {
					bestHit = faceHit;
					maxDistance = faceHit.distance;
				}
			}
		}

		if (accessoryHitMode != AccessoryHitMode.NONE && blockEntity != null) {
			for (int i = 0; i < blockEntity.getAccessories().size(); i++) {
				PlacedAccessory accessory = blockEntity.getAccessories().get(i);

					if (accessoryHitMode == AccessoryHitMode.STACKABLE_ONLY && !Kurasu.BOOK_1_ACCESSORY_ID.equals(accessory.accessoryId())) {
						continue;
					}

				Hit accessoryHit = raycastAccessory(blockPos, facing, accessory, i, start, rayDirection, maxDistance, doorPivot, doorAngle);

				if (accessoryHit != null && (bestHit == null || accessoryHit.distance < bestHit.distance)) {
					bestHit = accessoryHit;
					maxDistance = accessoryHit.distance;
				}
			}
		}

		return bestHit;
	}

	private static Hit raycastFace(BlockPos basePos, Direction facing, MeshFace face, Vec3 start, Vec3 rayDirection, double maxDistance, Vector3f doorPivot, float doorAngle) {
		if (face.vertices.size() != 4) {
			return null;
		}

		Vector3f[] localVerts = new Vector3f[4];
		Vector3f[] worldVerts = new Vector3f[4];

		for (int i = 0; i < 4; i++) {
			MeshVertex vertex = face.vertices.get(i);
			Vector3f position = new Vector3f(vertex.position);
			localVerts[i] = new Vector3f(vertex.position);

			if (doorPivot != null && "door".equalsIgnoreCase(face.partName)) {
				position.sub(doorPivot).rotateY(doorAngle).add(doorPivot);
			}

			worldVerts[i] = rotatePosition(position, facing).add(basePos.getX(), basePos.getY(), basePos.getZ());
		}

		Vector3f normal = new Vector3f(face.normal).normalize();
		return raycastFace(basePos, face.partName, -1, start, rayDirection, maxDistance, worldVerts, localVerts, normal);
	}

	private static Hit raycastFace(BlockPos basePos, String partName, int accessoryIndex, Vec3 start, Vec3 rayDirection, double maxDistance, Vector3f[] worldVerts, Vector3f[] localVerts, Vector3f normal) {
		Vector3f tangent = new Vector3f(localVerts[1]).sub(localVerts[0]);
		Vector3f bitangent = new Vector3f(localVerts[3]).sub(localVerts[0]);

		if (tangent.lengthSquared() <= 0.000001f || bitangent.lengthSquared() <= 0.000001f) {
			return null;
		}

		tangent.normalize();
		bitangent.normalize();
		Hit h0 = raycastTriangle(basePos, partName, accessoryIndex, start, rayDirection, maxDistance, worldVerts[0], worldVerts[1], worldVerts[2], localVerts[0], localVerts[1], localVerts[2], normal, tangent, bitangent);
		Hit h1 = raycastTriangle(basePos, partName, accessoryIndex, start, rayDirection, maxDistance, worldVerts[0], worldVerts[2], worldVerts[3], localVerts[0], localVerts[2], localVerts[3], normal, tangent, bitangent);

		if (h0 == null) {
			return h1;
		}

		if (h1 == null) {
			return h0;
		}

		return h0.distance < h1.distance ? h0 : h1;
	}

	private static Hit raycastAccessory(BlockPos basePos, Direction facing, PlacedAccessory accessory, int accessoryIndex, Vec3 start, Vec3 rayDirection, double maxDistance, Vector3f doorPivot, float doorAngle) {
		ObjMesh accessoryMesh = Kurasu.STICKY_NOTE_ACCESSORY_ID.equals(accessory.accessoryId()) ? ObjModels.stickyNoteMesh : ObjModels.book1Mesh;
		AccessoryTransform accessoryTransform = AccessoryTransform.create(accessory, accessoryMesh, facing, doorPivot, doorAngle);

		if (accessoryTransform == null) {
			return null;
		}

		Vector3f modelOrigin = new Vector3f(accessoryTransform.modelOrigin);
		Vector3f localN = new Vector3f(accessoryTransform.localN);
		Vector3f localT = new Vector3f(accessoryTransform.localT);
		Vector3f localB = new Vector3f(accessoryTransform.localB);
		Vector3f center = new Vector3f(accessoryTransform.center);
		Vector3f worldN = new Vector3f(accessoryTransform.worldN);
		Vector3f worldT = new Vector3f(accessoryTransform.worldT);
		Vector3f worldB = new Vector3f(accessoryTransform.worldB);
		Vector3f worldCenter = new Vector3f(accessoryTransform.worldCenter);
		Hit bestHit = null;

		for (MeshLayer layer : accessoryMesh.layers) {
			for (MeshFace face : layer.faces) {
				if (face.vertices.size() != 4) {
					continue;
				}

				Vector3f[] localVerts = new Vector3f[4];
				Vector3f[] worldVerts = new Vector3f[4];

				for (int i = 0; i < 4; i++) {
					MeshVertex vertex = face.vertices.get(i);
					Vector3f modelOffset = new Vector3f(vertex.position).sub(modelOrigin).mul(accessory.scale());
					Vector3f local = mapModelVector(modelOffset, new Vector3f(center), localT, localB, localN);
					Vector3f world = mapModelVector(modelOffset, new Vector3f(worldCenter), worldT, worldB, worldN);
					localVerts[i] = local;

					worldVerts[i] = world.add(basePos.getX(), basePos.getY(), basePos.getZ());
				}

				Vector3f normal = mapModelVector(new Vector3f(face.normal), new Vector3f(), localT, localB, localN).normalize();
				Hit hit = raycastFace(basePos, accessory.partName(), accessoryIndex, start, rayDirection, maxDistance, worldVerts, localVerts, normal);

				if (hit != null && (bestHit == null || hit.distance < bestHit.distance)) {
					bestHit = hit;
					maxDistance = hit.distance;
				}
			}
		}

		return bestHit;
	}

	private static Hit raycastTriangle(BlockPos basePos, String partName, int accessoryIndex, Vec3 start, Vec3 rayDir, double maxDistance, Vector3f wa, Vector3f wb, Vector3f wc, Vector3f la, Vector3f lb, Vector3f lc, Vector3f normal, Vector3f tangent, Vector3f bitangent) {
		Vector3f v0v1 = new Vector3f(wb).sub(wa);
		Vector3f v0v2 = new Vector3f(wc).sub(wa);
		Vector3f rayVec = new Vector3f((float) rayDir.x, (float) rayDir.y, (float) rayDir.z);
		Vector3f pvec = new Vector3f(rayVec).cross(v0v2);
		float det = v0v1.dot(pvec);

		if (Math.abs(det) < 0.000001f) {
			return null;
		}

		float invDet = 1.0f / det;
		Vector3f tvec = new Vector3f((float) (start.x - wa.x()), (float) (start.y - wa.y()), (float) (start.z - wa.z()));
		float u = invDet * tvec.dot(pvec);

		if (u < 0.0f || u > 1.0f) {
			return null;
		}

		Vector3f qvec = new Vector3f(tvec).cross(v0v1);
		float v = invDet * rayVec.dot(qvec);

		if (v < 0.0f || u + v > 1.0f) {
			return null;
		}

		double t = (double) invDet * v0v2.dot(qvec);

		if (t < 0.000001 || t > maxDistance) {
			return null;
		}

		float w = 1.0f - u - v;
		Vector3f localHit = new Vector3f(la).mul(w);
		localHit.add(new Vector3f(lb).mul(u));
		localHit.add(new Vector3f(lc).mul(v));

		return new Hit(basePos, partName, accessoryIndex, localHit, normal, tangent, bitangent, t);
	}

	private static Vector3f rotatePosition(Vector3f position, Direction facing) {
		return switch (facing) {
			case SOUTH -> new Vector3f(1.0f - position.x(), position.y(), 1.0f - position.z());
			case WEST -> new Vector3f(position.z(), position.y(), 1.0f - position.x());
			case EAST -> new Vector3f(1.0f - position.z(), position.y(), position.x());
			default -> new Vector3f(position);
		};
	}

	private static Vector3f mapModelVector(Vector3f modelVector, Vector3f target, Vector3f tangent, Vector3f bitangent, Vector3f normal) {
		return target.fma(modelVector.x(), tangent).fma(modelVector.y(), bitangent).fma(modelVector.z(), normal);
	}

	public static final class Hit {
		private final BlockPos blockPos;
		private final String partName;
		private final int accessoryIndex;
		private final Vector3f localPosition;
		private final Vector3f localNormal;
		private final Vector3f localTangent;
		private final Vector3f localBitangent;
		private final double distance;

		private Hit(BlockPos blockPos, String partName, int accessoryIndex, Vector3f localPosition, Vector3f localNormal, Vector3f localTangent, Vector3f localBitangent, double distance) {
			this.blockPos = blockPos;
			this.partName = partName;
			this.accessoryIndex = accessoryIndex;
			this.localPosition = localPosition;
			this.localNormal = localNormal;
			this.localTangent = localTangent;
			this.localBitangent = localBitangent;
			this.distance = distance;
		}

		public BlockPos blockPos() {
			return this.blockPos;
		}

		public boolean isAccessoryHit() {
			return this.accessoryIndex >= 0;
		}

		public int accessoryIndex() {
			return this.accessoryIndex;
		}

		public AccessoryBlockEntity.PlacedAccessory toPlacedAccessory(String accessoryId, ItemStack itemStack) {
			return new AccessoryBlockEntity.PlacedAccessory(accessoryId, this.partName, this.localPosition.x(), this.localPosition.y(), this.localPosition.z(), this.localNormal.x(), this.localNormal.y(), this.localNormal.z(), this.localTangent.x(), this.localTangent.y(), this.localTangent.z(), this.localBitangent.x(), this.localBitangent.y(), this.localBitangent.z(), 0.0f, 1.0f, itemStack, null);
		}
	}

	public enum AccessoryHitMode {
		NONE,
		STACKABLE_ONLY,
		ALL
	}
}

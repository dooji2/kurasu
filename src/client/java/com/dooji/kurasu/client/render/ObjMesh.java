package com.dooji.kurasu.client.render;

import com.dooji.kurasu.block.entity.AccessoryBlockEntity.PlacedAccessory;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public final class ObjMesh {
	final List<MeshLayer> layers;
	final List<MeshFace> blackboardFaces;
	final Vector3f doorPivot;
	final AABB bounds;

	ObjMesh(List<MeshLayer> layers, List<MeshFace> blackboardFaces, Vector3f doorPivot, AABB bounds) {
		this.layers = layers;
		this.blackboardFaces = blackboardFaces;
		this.doorPivot = doorPivot;
		this.bounds = bounds;
	}
}

final class MeshLayer {
	final Identifier textureId;
	final List<MeshFace> faces;

	MeshLayer(Identifier textureId, List<MeshFace> faces) {
		this.textureId = textureId;
		this.faces = faces;
	}
}

final class MeshFace {
	final String partName;
	final List<MeshVertex> vertices;
	final Vector3f normal;

	MeshFace(String partName, List<MeshVertex> vertices, Vector3f normal) {
		this.partName = partName;
		this.vertices = vertices;
		this.normal = normal;
	}
}

final class MeshVertex {
	final Vector3f position;
	final float u;
	final float v;

	MeshVertex(Vector3f position, float u, float v) {
		this.position = position;
		this.u = u;
		this.v = v;
	}
}

final class AccessoryTransform {
	final Vector3f modelOrigin;
	final Vector3f localN;
	final Vector3f localT;
	final Vector3f localB;
	final Vector3f center;
	final Vector3f worldN;
	final Vector3f worldT;
	final Vector3f worldB;
	final Vector3f worldCenter;

	private AccessoryTransform(
		Vector3f modelOrigin,
		Vector3f localN,
		Vector3f localT,
		Vector3f localB,
		Vector3f center,
		Vector3f worldN,
		Vector3f worldT,
		Vector3f worldB,
		Vector3f worldCenter
	) {
		this.modelOrigin = modelOrigin;
		this.localN = localN;
		this.localT = localT;
		this.localB = localB;
		this.center = center;
		this.worldN = worldN;
		this.worldT = worldT;
		this.worldB = worldB;
		this.worldCenter = worldCenter;
	}

	static AccessoryTransform create(PlacedAccessory accessory, ObjMesh accessoryMesh, Direction facing, Vector3f doorPivot, float doorAngle) {
		if (accessoryMesh == null || accessoryMesh.bounds == null) {
			return null;
		}

		Vector3f modelOrigin = new Vector3f((float) ((accessoryMesh.bounds.minX + accessoryMesh.bounds.maxX) * 0.5), (float) ((accessoryMesh.bounds.minY + accessoryMesh.bounds.maxY) * 0.5), (float) ((accessoryMesh.bounds.minZ + accessoryMesh.bounds.maxZ) * 0.5));
		Vector3f localPos = new Vector3f(accessory.localX(), accessory.localY(), accessory.localZ());
		Vector3f localN = new Vector3f(accessory.normalX(), accessory.normalY(), accessory.normalZ()).normalize();
		Vector3f localT = new Vector3f(accessory.tangentX(), accessory.tangentY(), accessory.tangentZ()).normalize();
		Vector3f localB = new Vector3f(accessory.bitangentX(), accessory.bitangentY(), accessory.bitangentZ()).normalize();
		localT.rotateAxis(accessory.rotation(), localN.x(), localN.y(), localN.z());
		localB.rotateAxis(accessory.rotation(), localN.x(), localN.y(), localN.z());
		float normalSize = (float) (accessoryMesh.bounds.maxZ - accessoryMesh.bounds.minZ);
		float offset = normalSize * 0.5f * accessory.scale() + 0.0015f;
		Vector3f center = new Vector3f(localPos).fma(offset, localN);
		Vector3f worldPos = new Vector3f(localPos);
		Vector3f worldN = new Vector3f(localN);
		Vector3f worldT = new Vector3f(localT);
		Vector3f worldB = new Vector3f(localB);

		if ("door".equalsIgnoreCase(accessory.partName())) {
			if (doorPivot != null) {
				worldPos.sub(doorPivot).rotateY(doorAngle).add(doorPivot);
			}

			worldN.rotateY(doorAngle);
			worldT.rotateY(doorAngle);
			worldB.rotateY(doorAngle);
		}

		worldPos = rotatePosition(worldPos, facing);
		worldN = rotateNormal(worldN, facing).normalize();
		worldT = rotateNormal(worldT, facing).normalize();
		worldB = rotateNormal(worldB, facing).normalize();
		Vector3f worldCenter = new Vector3f(worldPos).add(new Vector3f(worldN).mul(offset));
		return new AccessoryTransform(modelOrigin, localN, localT, localB, center, worldN, worldT, worldB, worldCenter);
	}

	private static Vector3f rotatePosition(Vector3f position, Direction facing) {
		return switch (facing) {
			case SOUTH -> new Vector3f(1.0f - position.x(), position.y(), 1.0f - position.z());
			case WEST -> new Vector3f(position.z(), position.y(), 1.0f - position.x());
			case EAST -> new Vector3f(1.0f - position.z(), position.y(), position.x());
			default -> new Vector3f(position);
		};
	}

	private static Vector3f rotateNormal(Vector3f normal, Direction facing) {
		return switch (facing) {
			case SOUTH -> new Vector3f(-normal.x(), normal.y(), -normal.z());
			case WEST -> new Vector3f(normal.z(), normal.y(), -normal.x());
			case EAST -> new Vector3f(-normal.z(), normal.y(), normal.x());
			default -> new Vector3f(normal);
		};
	}
}

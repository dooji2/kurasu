package com.dooji.kurasu.client.render;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.block.entity.AccessoryBlockEntity;
import com.dooji.kurasu.block.entity.AccessoryBlockEntity.PlacedAccessory;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
import com.dooji.kurasu.block.entity.LockerBlockEntity;
import com.dooji.kurasu.client.AccessoryPlacementClient;
import com.dooji.kurasu.client.DrawTextures;
import com.dooji.kurasu.item.DrawData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LockerBlockEntityRenderer<T extends AccessoryBlockEntity> implements BlockEntityRenderer<T, LockerBlockEntityRenderer.RenderState> {
	private final Function<T, ObjMesh> meshGetter;

	public LockerBlockEntityRenderer(Function<T, ObjMesh> meshGetter) {
		this.meshGetter = meshGetter;
	}

	@Override
	public RenderState createRenderState() {
		return new RenderState();
	}

	@Override
	public void extractRenderState(T blockEntity, RenderState renderState, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay damageOverlayState) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, damageOverlayState);
		renderState.mesh = blockEntity.getLevel() != null ? ObjModels.getSurfaceMesh(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState()) : this.meshGetter.apply(blockEntity);
		renderState.facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
		renderState.openProgress = blockEntity instanceof LockerBlockEntity lockerBlockEntity ? lockerBlockEntity.getOpenProgress(partialTick) : 0.0f;
		renderState.accessories = blockEntity.getAccessories();
		renderState.previewAccessory = AccessoryPlacementClient.getPreview(blockEntity.getBlockPos());
		renderState.blackboardDraw = blockEntity instanceof BlackboardBlockEntity blackboardBlockEntity ? blackboardBlockEntity.getDrawData() : null;
	}

	@Override
	public void submit(RenderState renderState, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraRenderState) {
		ObjMesh lockerMesh = renderState.mesh;

		if (lockerMesh == null) {
			return;
		}

		Vector3f doorPivot = lockerMesh.doorPivot;
		float easedOpen = renderState.openProgress * renderState.openProgress * (3.0f - 2.0f * renderState.openProgress);
		float doorAngle = easedOpen * (float) Math.toRadians(105.0);

		for (MeshLayer layer : lockerMesh.layers) {
			renderTasks.submitCustomGeometry(poseStack, RenderTypes.entityCutout(layer.textureId), (pose, vertexConsumer) -> {
				for (MeshFace face : layer.faces) {
					renderLockerFace(face, renderState.facing, pose, vertexConsumer, renderState.lightCoords, doorPivot, doorAngle);
				}
			});
		}

		if (renderState.blackboardDraw != null && renderState.blackboardDraw.hasPixels()) {
			Identifier textureId = DrawTextures.getBlackboardTexture(renderState.blackboardDraw);
			renderTasks.submitCustomGeometry(poseStack, RenderTypes.entityCutout(textureId), (pose, vertexConsumer) -> {
				for (MeshFace face : lockerMesh.blackboardFaces) {
					renderBlackboardFace(face, renderState.facing, pose, vertexConsumer, renderState.lightCoords);
				}
			});
		}

		if (!renderState.accessories.isEmpty() || renderState.previewAccessory != null) {
			for (PlacedAccessory accessory : renderState.accessories) {
				renderAccessory(renderTasks, poseStack, accessory, renderState.facing, renderState.lightCoords, doorPivot, doorAngle);
			}

			if (renderState.previewAccessory != null) {
				renderAccessory(renderTasks, poseStack, renderState.previewAccessory, renderState.facing, renderState.lightCoords, doorPivot, doorAngle);
			}
		}
	}

	private void renderLockerFace(MeshFace face, Direction facing, PoseStack.Pose pose, VertexConsumer vertexConsumer, int lightCoords, Vector3f doorPivot, float doorAngle) {
		boolean isDoor = "door".equalsIgnoreCase(face.partName);
		Vector3f normal = new Vector3f(face.normal);

		if (isDoor) {
			normal.rotateY(doorAngle);
		}

		normal = rotateNormal(normal, facing).normalize();

		for (int i = 0; i < face.vertices.size(); i++) {
			MeshVertex vertex = face.vertices.get(i);
			Vector3f position = new Vector3f(vertex.position);

			if (isDoor && doorPivot != null) {
				position.sub(doorPivot).rotateY(doorAngle).add(doorPivot);
			}

			position = rotatePosition(position, facing).fma(0.0005f, normal);
			Vector4f transformed = pose.pose().transform(new Vector4f(position.x(), position.y(), position.z(), 1.0f));

			vertexConsumer.addVertex(transformed.x(), transformed.y(), transformed.z(), -1, vertex.u, vertex.v, OverlayTexture.NO_OVERLAY, lightCoords, normal.x(), normal.y(), normal.z());
		}
	}

	private void renderBlackboardFace(MeshFace face, Direction facing, PoseStack.Pose pose, VertexConsumer vertexConsumer, int lightCoords) {
		Vector3f normal = rotateNormal(new Vector3f(face.normal), facing).normalize();
		Vector3f offset = new Vector3f(normal).mul(0.001f);

		for (int i = 0; i < face.vertices.size(); i++) {
			MeshVertex vertex = face.vertices.get(i);
			Vector3f position = rotatePosition(new Vector3f(vertex.position), facing).add(offset);
			Vector4f transformed = pose.pose().transform(new Vector4f(position.x(), position.y(), position.z(), 1.0f));
			vertexConsumer.addVertex(transformed.x(), transformed.y(), transformed.z(), -1, vertex.u, vertex.v, OverlayTexture.NO_OVERLAY, lightCoords, normal.x(), normal.y(), normal.z());
		}
	}

	private void renderAccessory(SubmitNodeCollector renderTasks, PoseStack poseStack, PlacedAccessory accessory, Direction facing, int lightCoords, Vector3f doorPivot, float doorAngle) {
		ObjMesh accessoryMesh = Kurasu.STICKY_NOTE_ACCESSORY_ID.equals(accessory.accessoryId()) ? ObjModels.stickyNoteMesh : ObjModels.book1Mesh;
		AccessoryTransform accessoryTransform = AccessoryTransform.create(accessory, accessoryMesh, facing, doorPivot, doorAngle);

		if (accessoryTransform == null) {
			return;
		}

		for (MeshLayer layer : accessoryMesh.layers) {
			Identifier textureId = Kurasu.STICKY_NOTE_ACCESSORY_ID.equals(accessory.accessoryId()) ? DrawTextures.getStickyNoteTexture(accessory.drawData()) : layer.textureId;
			renderTasks.submitCustomGeometry(poseStack, RenderTypes.entityCutout(textureId), (pose, vertexConsumer) -> {
				for (MeshFace face : layer.faces) {
					renderAccessoryFace(accessoryTransform, accessory.scale(), face, lightCoords, pose, vertexConsumer);
				}
			});
		}
	}

	private void renderAccessoryFace(AccessoryTransform accessoryTransform, float scale, MeshFace face, int lightCoords, PoseStack.Pose pose, VertexConsumer vertexConsumer) {
		Vector3f faceNormal = mapModelVector(new Vector3f(face.normal), new Vector3f(), accessoryTransform.worldT, accessoryTransform.worldB, accessoryTransform.worldN).normalize();

		for (int i = 0; i < face.vertices.size(); i++) {
			MeshVertex vertex = face.vertices.get(i);
			Vector3f modelOffset = new Vector3f(vertex.position).sub(accessoryTransform.modelOrigin).mul(scale);
			Vector3f worldVertex = mapModelVector(modelOffset, new Vector3f(accessoryTransform.worldCenter), accessoryTransform.worldT, accessoryTransform.worldB, accessoryTransform.worldN).fma(0.0005f, faceNormal);
			Vector4f transformed = pose.pose().transform(new Vector4f(worldVertex.x(), worldVertex.y(), worldVertex.z(), 1.0f));
			vertexConsumer.addVertex(transformed.x(), transformed.y(), transformed.z(), -1, vertex.u, vertex.v, OverlayTexture.NO_OVERLAY, lightCoords, faceNormal.x(), faceNormal.y(), faceNormal.z());
		}
	}

	private Vector3f mapModelVector(Vector3f modelVector, Vector3f target, Vector3f tangent, Vector3f bitangent, Vector3f normal) {
		return target.fma(modelVector.x(), tangent).fma(modelVector.y(), bitangent).fma(modelVector.z(), normal);
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

	public static class RenderState extends BlockEntityRenderState {
		public ObjMesh mesh;
		public Direction facing = Direction.NORTH;
		public float openProgress;
		public List<PlacedAccessory> accessories = List.of();
		public PlacedAccessory previewAccessory;
		public DrawData blackboardDraw;
	}
}

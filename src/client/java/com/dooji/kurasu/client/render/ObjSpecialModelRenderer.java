package com.dooji.kurasu.client.render;

import com.dooji.kurasu.Kurasu;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class ObjSpecialModelRenderer implements NoDataSpecialModelRenderer {
	private static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "obj_block_item");
	private final Identifier modelId;

	public ObjSpecialModelRenderer(Identifier modelId) {
		this.modelId = modelId;
	}

	public static void init() {
		SpecialModelRenderers.ID_MAPPER.put(TYPE_ID, Unbaked.MAP_CODEC);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector renderTasks, int lightCoords, int overlay, boolean foil, int color) {
		ObjMesh mesh = ObjModels.getItemMesh(this.modelId);

		if (mesh == null) {
			return;
		}

		poseStack.pushPose();
		poseStack.mulPose(centerModelTransform(mesh));

		for (MeshLayer layer : mesh.layers) {
			renderTasks.submitCustomGeometry(poseStack, RenderTypes.entityCutout(layer.textureId), (pose, vertexConsumer) -> {
				for (MeshFace face : layer.faces) {
					Vector3f normal = pose.transformNormal(new Vector3f(face.normal), new Vector3f()).normalize();

					for (MeshVertex vertex : face.vertices) {
						Vector4f transformed = pose.pose().transform(new Vector4f(vertex.position.x(), vertex.position.y(), vertex.position.z(), 1.0f));
						vertexConsumer.addVertex(transformed.x() + normal.x() * 0.0005f, transformed.y() + normal.y() * 0.0005f, transformed.z() + normal.z() * 0.0005f, -1, vertex.u, vertex.v, overlay, lightCoords, normal.x(), normal.y(), normal.z());
					}
				}
			});
		}

		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> consumer) {
		ObjMesh mesh = ObjModels.getItemMesh(this.modelId);

		if (mesh == null || mesh.bounds == null) {
			return;
		}

		Matrix4f transform = centerModelTransform(mesh);

		for (float x : new float[] {(float) mesh.bounds.minX, (float) mesh.bounds.maxX}) {
			for (float y : new float[] {(float) mesh.bounds.minY, (float) mesh.bounds.maxY}) {
				for (float z : new float[] {(float) mesh.bounds.minZ, (float) mesh.bounds.maxZ}) {
					Vector4f corner = transform.transform(new Vector4f(x, y, z, 1.0f));
					consumer.accept(new Vector3f(corner.x(), corner.y(), corner.z()));
				}
			}
		}
	}

	private Matrix4f centerModelTransform(ObjMesh mesh) {
		if (mesh.bounds == null) {
			return new Matrix4f().translate(0.5f, 0.5f, 0.5f);
		}

		float centerX = (float) ((mesh.bounds.minX + mesh.bounds.maxX) * 0.5);
		float centerY = (float) ((mesh.bounds.minY + mesh.bounds.maxY) * 0.5);
		float centerZ = (float) ((mesh.bounds.minZ + mesh.bounds.maxZ) * 0.5);
		float sizeX = (float) (mesh.bounds.maxX - mesh.bounds.minX);
		float sizeY = (float) (mesh.bounds.maxY - mesh.bounds.minY);
		float sizeZ = (float) (mesh.bounds.maxZ - mesh.bounds.minZ);
		float maxSize = Math.max(sizeX, Math.max(sizeY, sizeZ));
		float scale = maxSize > 0.0f ? 0.9f / maxSize : 1.0f;
		boolean flipBook = "book_1".equals(this.modelId.getPath());

		return new Matrix4f()
			.translate(0.5f, 0.5f, 0.5f)
			.rotateY(flipBook ? (float) Math.toRadians(180.0) : 0.0f)
			.scale(scale)
			.translate(-centerX, -centerY, -centerZ);
	}

	public record Unbaked(Identifier modelId) implements SpecialModelRenderer.Unbaked<Void> {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Identifier.CODEC.fieldOf("model_id").forGetter(Unbaked::modelId)
		).apply(instance, Unbaked::new));

		@Override
		public NoDataSpecialModelRenderer bake(SpecialModelRenderer.BakingContext bakingContext) {
			return new ObjSpecialModelRenderer(this.modelId);
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}
}

package com.dooji.kurasu.client.render;

import com.dooji.kurasu.KurasuBlocks;
import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.block.BlackboardBlock;
import com.dooji.kurasu.block.ChairBlock;
import com.dooji.kurasu.block.DeskBlock;
import com.dooji.kurasu.block.LockerBlock;
import com.dooji.kurasu.block.SafeBlock;
import com.dooji.kurasu.client.DrawTextures;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class ObjModels {
	private static final Identifier LOCKER_SINGLE_MODEL_ID = modelId("locker_single");
	private static final Identifier LOCKER_DOUBLE_MODEL_ID = modelId("locker_double");
	private static final Identifier LOCKER_BOTTOM_MODEL_ID = modelId("locker_bottom");
	private static final Identifier LOCKER_MIDDLE_MODEL_ID = modelFileId("locker_middle_a_1", "locker_middle.obj");
	private static final Identifier LOCKER_MIDDLE_1_MODEL_ID = modelFileId("locker_middle_b_1", "locker_middle_1.obj");
	private static final Identifier LOCKER_TOP_MODEL_ID = modelId("locker_top");
	private static final Identifier LOCKER_SINGLE_1_MODEL_ID = modelFileId("locker_single_1", "locker_single.obj");
	private static final Identifier LOCKER_DOUBLE_1_MODEL_ID = modelFileId("locker_double_1", "locker_double.obj");
	private static final Identifier LOCKER_BOTTOM_1_MODEL_ID = modelFileId("locker_bottom_1", "locker_bottom.obj");
	private static final Identifier LOCKER_MIDDLE_A_2_MODEL_ID = modelFileId("locker_middle_a_2", "locker_middle.obj");
	private static final Identifier LOCKER_MIDDLE_B_2_MODEL_ID = modelFileId("locker_middle_b_2", "locker_middle_1.obj");
	private static final Identifier LOCKER_TOP_1_MODEL_ID = modelFileId("locker_top_1", "locker_top.obj");
	private static final Identifier SAFE_MODEL_ID = modelId("safe");
	private static final Identifier BLACKBOARD_MODEL_ID = modelId("blackboard");
	private static final Identifier BLACKBOARD_1_MODEL_ID = modelFileId("blackboard_1", "blackboard.obj");
	private static final Identifier CHAIR_MODEL_ID = modelId("chair");
	private static final Identifier CHAIR_1_MODEL_ID = modelFileId("chair_1", "chair.obj");
	private static final Identifier DESK_MODEL_ID = modelId("desk");
	private static final Identifier DESK_1_MODEL_ID = modelFileId("desk_1", "desk.obj");
	private static final Identifier STICKY_NOTE_MODEL_ID = modelId("sticky_note");
	private static final Identifier BOOK_1_MODEL_ID = modelId("book_1");
	private static final Identifier BLACKBOARD_DRAW_TEXTURE_ID = modelFileId("blackboard", "blackboad.png");
	private static final Identifier STICKY_NOTE_DRAW_TEXTURE_ID = modelFileId("sticky_note", "sprite-0008-export.png");
	private static ObjMesh lockerSingleMesh;
	private static ObjMesh lockerDoubleMesh;
	private static ObjMesh lockerBottomMesh;
	private static ObjMesh lockerMiddleMesh;
	private static ObjMesh lockerMiddle1Mesh;
	private static ObjMesh lockerTopMesh;
	private static ObjMesh lockerSingle1Mesh;
	private static ObjMesh lockerDouble1Mesh;
	private static ObjMesh lockerBottom1Mesh;
	private static ObjMesh lockerMiddleA2Mesh;
	private static ObjMesh lockerMiddleB2Mesh;
	private static ObjMesh lockerTop1Mesh;
	private static ObjMesh safeMesh;
	private static ObjMesh blackboardMesh;
	private static ObjMesh blackboard1Mesh;
	private static ObjMesh chairMesh;
	private static ObjMesh chair1Mesh;
	private static ObjMesh deskMesh;
	private static ObjMesh desk1Mesh;
	static ObjMesh stickyNoteMesh;
	static ObjMesh book1Mesh;

	public static void init() {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "obj_models"),
			new SimpleReloadListener<ResourceManager>() {
				@Override
				protected ResourceManager prepare(SharedState state) {
					return state.resourceManager();
				}

				@Override
				protected void apply(ResourceManager resourceManager, SharedState state) {
					try {
						ObjModels.reload(resourceManager);
					} catch (IOException exception) {
						throw new RuntimeException(exception);
					}
				}
			}
		);
	}

	private static ObjMesh getLockerMesh(LockerBlock.Part part) {
		return switch (part) {
			case SINGLE -> lockerSingleMesh;
			case BOTTOM -> lockerBottomMesh;
			case MIDDLE -> lockerMiddleMesh;
			case MIDDLE_1 -> lockerMiddle1Mesh;
			case TOP -> lockerTopMesh;
		};
	}

	private static ObjMesh getLockerMesh(BlockState state, LockerBlock.Part part) {
		if (state.getBlock() == KurasuBlocks.LOCKER_1) {
			return switch (part) {
				case SINGLE -> lockerSingle1Mesh;
				case BOTTOM -> lockerBottom1Mesh;
				case MIDDLE -> lockerMiddleA2Mesh;
				case MIDDLE_1 -> lockerMiddleB2Mesh;
				case TOP -> lockerTop1Mesh;
			};
		}

		return getLockerMesh(part);
	}

	public static ObjMesh getItemMesh(Identifier blockId) {
		String path = blockId.getPath();

		return switch (path) {
			case "locker" -> lockerSingleMesh;
			case "locker_1" -> lockerSingle1Mesh;
			case "safe" -> safeMesh;
			case "blackboard" -> blackboardMesh;
			case "blackboard_1" -> blackboard1Mesh;
			case "chair" -> chairMesh;
			case "chair_1" -> chair1Mesh;
			case "desk" -> deskMesh;
			case "desk_1" -> desk1Mesh;
			case "sticky_note" -> stickyNoteMesh;
			case "book_1" -> book1Mesh;
			default -> null;
		};
	}

	public static int[] blackboardPixel(Vector3f localPosition) {
		if (blackboardMesh == null || blackboardMesh.blackboardFaces.isEmpty()) {
			return null;
		}

		MeshFace face = blackboardMesh.blackboardFaces.getFirst();
		if (face.vertices.size() != 4) {
			return null;
		}

		MeshVertex origin = face.vertices.get(0);
		MeshVertex tangentVertex = face.vertices.get(1);
		MeshVertex bitangentVertex = face.vertices.get(3);
		Vector3f tangent = new Vector3f(tangentVertex.position).sub(origin.position);
		Vector3f bitangent = new Vector3f(bitangentVertex.position).sub(origin.position);
		float tangentLengthSquared = tangent.lengthSquared();
		float bitangentLengthSquared = bitangent.lengthSquared();

		if (tangentLengthSquared <= 0.000001f || bitangentLengthSquared <= 0.000001f) {
			return null;
		}

		Vector3f offset = new Vector3f(localPosition).sub(origin.position);
		float uLerp = offset.dot(tangent) / tangentLengthSquared;
		float vLerp = offset.dot(bitangent) / bitangentLengthSquared;
		float u = Mth.lerp(Mth.clamp(uLerp, 0.0f, 1.0f), origin.u, tangentVertex.u);
		float v = Mth.lerp(Mth.clamp(vLerp, 0.0f, 1.0f), origin.v, bitangentVertex.v);
		int x = Mth.clamp((int) (u * 60.0f), 0, 59);
		int y = Mth.clamp((int) (v * 28.0f), 0, 27);
		return new int[] {x, y};
	}

	public static ObjMesh getSurfaceMesh(BlockState state) {
		if (state.getBlock() instanceof LockerBlock) {
			return getLockerMesh(state, state.getValue(LockerBlock.PART));
		}

		if (state.getBlock() instanceof SafeBlock) {
			return safeMesh;
		}

		if (state.getBlock() instanceof BlackboardBlock) {
			if (state.getBlock() == KurasuBlocks.BLACKBOARD_1) {
				return blackboard1Mesh;
			}

			return blackboardMesh;
		}

		if (state.getBlock() instanceof ChairBlock) {
			if (state.getBlock() == KurasuBlocks.CHAIR_1) {
				return chair1Mesh;
			}

			return chairMesh;
		}

		if (state.getBlock() instanceof DeskBlock) {
			if (state.getBlock() == KurasuBlocks.DESK_1) {
				return desk1Mesh;
			}

			return deskMesh;
		}

		return null;
	}

	static ObjMesh getSurfaceMesh(LevelReader level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof LockerBlock)) {
			return getSurfaceMesh(state);
		}

		if (isDoubleTop(level, pos, state)) {
			return null;
		}

		if (isDoubleBottom(level, pos, state)) {
			return state.getBlock() == KurasuBlocks.LOCKER_1 ? lockerDouble1Mesh : lockerDoubleMesh;
		}

		return getLockerMesh(state, state.getValue(LockerBlock.PART));
	}

	private static boolean isDoubleBottom(LevelReader level, BlockPos pos, BlockState state) {
		if (state.getValue(LockerBlock.PART) != LockerBlock.Part.BOTTOM) {
			return false;
		}

		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		return matchesLockerNeighbor(level.getBlockState(pos.above()), state, facing) && !matchesLockerNeighbor(level.getBlockState(pos.above().above()), state, facing);
	}

	private static boolean isDoubleTop(LevelReader level, BlockPos pos, BlockState state) {
		if (state.getValue(LockerBlock.PART) != LockerBlock.Part.TOP) {
			return false;
		}

		Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		return matchesLockerNeighbor(level.getBlockState(pos.below()), state, facing) && !matchesLockerNeighbor(level.getBlockState(pos.below().below()), state, facing);
	}

	private static boolean matchesLockerNeighbor(BlockState neighborState, BlockState state, Direction facing) {
		return neighborState.getBlock() == state.getBlock() && neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
	}

	private static void reload(ResourceManager resourceManager) throws IOException {
		DrawTextures.clear();
		DrawTextures.setBlackboardBase(loadTextureImage(resourceManager, BLACKBOARD_DRAW_TEXTURE_ID));
		DrawTextures.setStickyNoteBase(loadTextureImage(resourceManager, STICKY_NOTE_DRAW_TEXTURE_ID));
		lockerSingleMesh = loadObjModel(resourceManager, LOCKER_SINGLE_MODEL_ID);
		lockerDoubleMesh = loadObjModel(resourceManager, LOCKER_DOUBLE_MODEL_ID);
		lockerBottomMesh = loadObjModel(resourceManager, LOCKER_BOTTOM_MODEL_ID);
		lockerMiddleMesh = loadObjModel(resourceManager, LOCKER_MIDDLE_MODEL_ID);
		lockerMiddle1Mesh = loadObjModel(resourceManager, LOCKER_MIDDLE_1_MODEL_ID);
		lockerTopMesh = loadObjModel(resourceManager, LOCKER_TOP_MODEL_ID);
		lockerSingle1Mesh = loadObjModel(resourceManager, LOCKER_SINGLE_1_MODEL_ID);
		lockerDouble1Mesh = loadObjModel(resourceManager, LOCKER_DOUBLE_1_MODEL_ID);
		lockerBottom1Mesh = loadObjModel(resourceManager, LOCKER_BOTTOM_1_MODEL_ID);
		lockerMiddleA2Mesh = loadObjModel(resourceManager, LOCKER_MIDDLE_A_2_MODEL_ID);
		lockerMiddleB2Mesh = loadObjModel(resourceManager, LOCKER_MIDDLE_B_2_MODEL_ID);
		lockerTop1Mesh = loadObjModel(resourceManager, LOCKER_TOP_1_MODEL_ID);
		safeMesh = loadObjModel(resourceManager, SAFE_MODEL_ID);
		blackboardMesh = loadObjModel(resourceManager, BLACKBOARD_MODEL_ID);
		blackboard1Mesh = loadObjModel(resourceManager, BLACKBOARD_1_MODEL_ID);
		chairMesh = loadObjModel(resourceManager, CHAIR_MODEL_ID);
		chair1Mesh = loadObjModel(resourceManager, CHAIR_1_MODEL_ID);
		deskMesh = loadObjModel(resourceManager, DESK_MODEL_ID);
		desk1Mesh = loadObjModel(resourceManager, DESK_1_MODEL_ID);
		stickyNoteMesh = loadObjModel(resourceManager, STICKY_NOTE_MODEL_ID);
		book1Mesh = loadObjModel(resourceManager, BOOK_1_MODEL_ID);
	}

	private static ObjMesh loadObjModel(ResourceManager resourceManager, Identifier objId) throws IOException {
		List<Vector3f> positions = new ArrayList<>();
		List<float[]> uvs = new ArrayList<>();
		List<Vector3f> normals = new ArrayList<>();
		Map<String, Identifier> materialTextures = new HashMap<>();
		Map<Identifier, List<MeshFace>> facesByTexture = new LinkedHashMap<>();
		List<MeshFace> blackboardFaces = new ArrayList<>();
		AABB.Builder bounds = new AABB.Builder();
		float[] doorBounds = null;
		String currentObject = "";
		String currentMaterial = null;

		loadMtl(resourceManager, objId, materialTextures);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.open(objId), StandardCharsets.UTF_8))) {
			String line;

			while ((line = reader.readLine()) != null) {
				line = stripComment(line).trim();

				if (line.isEmpty()) {
					continue;
				}

				if (line.startsWith("usemtl ")) {
					currentMaterial = line.substring(7).trim();
					continue;
				}

				if (line.startsWith("o ")) {
					currentObject = line.substring(2).trim();
					continue;
				}

				if (line.startsWith("v ")) {
					positions.add(parseObjVector(line.substring(2).trim()));
					continue;
				}

				if (line.startsWith("vt ")) {
					uvs.add(parseObjUv(line.substring(3).trim()));
					continue;
				}

				if (line.startsWith("vn ")) {
					normals.add(parseObjVector(line.substring(3).trim()).normalize());
					continue;
				}

				if (!line.startsWith("f ") || "none".equals(currentMaterial)) {
					continue;
				}

				Identifier textureId = materialTextures.get(currentMaterial);
				MeshFace face = buildObjFace(currentObject, line.substring(2).trim(), positions, uvs, normals);

				facesByTexture.computeIfAbsent(textureId, key -> new ArrayList<>()).add(face);
				if ("board_surface".equalsIgnoreCase(face.partName) && face.normal.z() <= -0.9f) {
					blackboardFaces.add(face);
				}

				for (MeshVertex vertex : face.vertices) {
					bounds.include(vertex.position);
				}

				if ("door".equalsIgnoreCase(currentObject)) {
					doorBounds = includeBounds(doorBounds, face.vertices);
				}
			}
		}

		List<MeshLayer> layers = new ArrayList<>();

		for (Map.Entry<Identifier, List<MeshFace>> entry : facesByTexture.entrySet()) {
			layers.add(new MeshLayer(entry.getKey(), List.copyOf(entry.getValue())));
		}

		AABB modelBounds = bounds.isDefined() ? bounds.build() : null;
		return new ObjMesh(List.copyOf(layers), List.copyOf(blackboardFaces), fixedDoorPivot(doorBounds), modelBounds);
	}

	private static void loadMtl(ResourceManager resourceManager, Identifier objId, Map<String, Identifier> materialTextures) throws IOException {
		Identifier mtlId = siblingIdentifier(objId, "materials.mtl");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.open(mtlId), StandardCharsets.UTF_8))) {
			String currentMaterial = null;
			String line;

			while ((line = reader.readLine()) != null) {
				line = stripComment(line).trim();

				if (line.isEmpty()) {
					continue;
				}

				if (line.startsWith("newmtl ")) {
					currentMaterial = line.substring(7).trim();
					continue;
				}

				if (currentMaterial != null && line.startsWith("map_Kd ")) {
					String textureName = line.substring(7).trim();
					materialTextures.put(currentMaterial, siblingIdentifier(objId, textureName));
					continue;
				}
			}
		}
	}

	private static MeshFace buildObjFace(String partName, String data, List<Vector3f> positions, List<float[]> uvs, List<Vector3f> normals) {
		String[] tokens = data.split("\\s+");
		int[] firstRef = parseObjRef(tokens[0]);
		int[] secondRef = parseObjRef(tokens[1]);
		int[] thirdRef = parseObjRef(tokens[2]);
		int[] fourthRef = parseObjRef(tokens[3]);
		float[] firstUv = uvs.get(firstRef[1]);
		float[] secondUv = uvs.get(secondRef[1]);
		float[] thirdUv = uvs.get(thirdRef[1]);
		float[] fourthUv = uvs.get(fourthRef[1]);
		return new MeshFace(
			partName,
			List.of(
				new MeshVertex(toMeshPosition(positions.get(firstRef[0])), firstUv[0], firstUv[1]),
				new MeshVertex(toMeshPosition(positions.get(secondRef[0])), secondUv[0], secondUv[1]),
				new MeshVertex(toMeshPosition(positions.get(thirdRef[0])), thirdUv[0], thirdUv[1]),
				new MeshVertex(toMeshPosition(positions.get(fourthRef[0])), fourthUv[0], fourthUv[1])
			),
			new Vector3f(normals.get(firstRef[2]))
		);
	}

	private static int[] parseObjRef(String token) {
		String[] parts = token.split("/", -1);
		int positionIndex = Integer.parseInt(parts[0]) - 1;
		int uvIndex = Integer.parseInt(parts[1]) - 1;
		int normalIndex = Integer.parseInt(parts[2]) - 1;

		return new int[] {positionIndex, uvIndex, normalIndex};
	}

	private static Vector3f toMeshPosition(Vector3f position) {
		return new Vector3f(position.x() + 0.5f, position.y(), position.z() + 0.5f);
	}

	private static float[] includeBounds(float[] bounds, List<MeshVertex> vertices) {
		if (bounds == null) {
			bounds = new float[] {
				Float.POSITIVE_INFINITY,
				Float.POSITIVE_INFINITY,
				Float.POSITIVE_INFINITY,
				Float.NEGATIVE_INFINITY,
				Float.NEGATIVE_INFINITY,
				Float.NEGATIVE_INFINITY
			};
		}

		for (MeshVertex vertex : vertices) {
			Vector3f position = vertex.position;
			bounds[0] = Math.min(bounds[0], position.x());
			bounds[1] = Math.min(bounds[1], position.y());
			bounds[2] = Math.min(bounds[2], position.z());
			bounds[3] = Math.max(bounds[3], position.x());
			bounds[4] = Math.max(bounds[4], position.y());
			bounds[5] = Math.max(bounds[5], position.z());
		}

		return bounds;
	}

	private static Vector3f fixedDoorPivot(float[] bounds) {
		if (bounds == null) {
			return null;
		}

		float hingeY = Math.min(bounds[1] + 1.0f / 16.0f, bounds[4]);
		float hingeZ = (bounds[2] + bounds[5]) * 0.5f;
		return new Vector3f(bounds[0], hingeY, hingeZ);
	}

	private static float[] parseObjUv(String data) {
		String[] parts = data.split("\\s+");
		return new float[] {Float.parseFloat(parts[0]), 1.0f - Float.parseFloat(parts[1])};
	}

	private static Vector3f parseObjVector(String data) {
		String[] parts = data.split("\\s+");
		return new Vector3f(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
	}

	private static String stripComment(String line) {
		int commentIndex = line.indexOf('#');
		return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
	}

	private static Identifier siblingIdentifier(Identifier baseId, String name) {
		Path basePath = Path.of(baseId.getPath()).getParent();
		Path resolvedPath = basePath == null ? Path.of(name.trim()) : basePath.resolve(name.trim());
		return Identifier.fromNamespaceAndPath(baseId.getNamespace(), resolvedPath.toString().replace('\\', '/'));
	}

	private static NativeImage loadTextureImage(ResourceManager resourceManager, Identifier textureId) throws IOException {
		return NativeImage.read(resourceManager.open(textureId));
	}

	private static Identifier modelId(String name) {
		return Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "models/obj/" + name + "/" + name + ".obj");
	}

	private static Identifier modelFileId(String name, String fileName) {
		return Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "models/obj/" + name + "/" + fileName);
	}
}

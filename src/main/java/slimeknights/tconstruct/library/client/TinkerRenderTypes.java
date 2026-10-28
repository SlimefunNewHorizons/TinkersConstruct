package slimeknights.tconstruct.library.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import slimeknights.tconstruct.TConstruct;

/** Render types used by Tinkers, pipelines are registered in {@link #registerPipelines(RegisterRenderPipelinesEvent)} */
public class TinkerRenderTypes {
  private TinkerRenderTypes() {}

  /** Lines seen through everything, mostly based on {@link RenderPipelines#LINES} */
  private static final RenderPipeline ERROR_BLOCK_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
    .withLocation(TConstruct.getResource("pipeline/error_block"))
    .withBlend(BlendFunction.TRANSLUCENT)
    .withCull(false)
    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
    .build();

  /** Fluids with both sides visible, based on {@link RenderPipelines#TRANSLUCENT_MOVING_BLOCK} */
  private static final RenderPipeline SMELTERY_FLUID_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
    .withLocation(TConstruct.getResource("pipeline/smeltery_fluid"))
    .withVertexShader("core/rendertype_translucent_moving_block")
    .withFragmentShader("core/rendertype_translucent_moving_block")
    .withSampler("Sampler0")
    .withSampler("Sampler2")
    .withBlend(BlendFunction.TRANSLUCENT)
    .withCull(false)
    .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
    .build();

  /** Render type for the error block that is seen through everything */
  public static final RenderType ERROR_BLOCK = RenderType.create(TConstruct.resourceString("lines"),
    RenderSetup.builder(ERROR_BLOCK_PIPELINE)
               .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
               .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
               .createRenderSetup());

  /** Render type for fluids, like {@link slimeknights.mantle.client.render.MantleRenderTypes#FLUID}, but disables cull so both sides show */
  public static final RenderType SMELTERY_FLUID = RenderType.create(TConstruct.resourceString("smeltery_fluid"),
    RenderSetup.builder(SMELTERY_FLUID_PIPELINE)
               .useLightmap()
               .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
               .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
               .sortOnUpload()
               .createRenderSetup());

  /** Registers the custom pipelines */
  public static void registerPipelines(RegisterRenderPipelinesEvent event) {
    event.registerPipeline(ERROR_BLOCK_PIPELINE);
    event.registerPipeline(SMELTERY_FLUID_PIPELINE);
  }
}

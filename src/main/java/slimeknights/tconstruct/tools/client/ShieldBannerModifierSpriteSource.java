package slimeknights.tconstruct.tools.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map.Entry;

/** Sprite source creating modifier textures for banners using shield banner textures */
public record ShieldBannerModifierSpriteSource(int cropX, int cropY, int cropWidth, int cropHeight, Identifier destinationPrefix, int offsetX, int offsetY, int outSize) implements SpriteSource {
  private static final Codec<Integer> NON_NEGATIVE = ExtraCodecs.intRange(0, Integer.MAX_VALUE);
  private static final Codec<Integer> SHIELD_SIZE = ExtraCodecs.intRange(0, 64);
  public static final MapCodec<ShieldBannerModifierSpriteSource> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    SHIELD_SIZE.fieldOf("crop_x").forGetter(ShieldBannerModifierSpriteSource::cropX),
    SHIELD_SIZE.fieldOf("crop_y").forGetter(ShieldBannerModifierSpriteSource::cropY),
    SHIELD_SIZE.fieldOf("crop_width").forGetter(ShieldBannerModifierSpriteSource::cropWidth),
    SHIELD_SIZE.fieldOf("crop_height").forGetter(ShieldBannerModifierSpriteSource::cropHeight),
    Identifier.CODEC.fieldOf("destination_prefix").forGetter(ShieldBannerModifierSpriteSource::destinationPrefix),
    NON_NEGATIVE.fieldOf("offset_x").forGetter(ShieldBannerModifierSpriteSource::offsetX),
    NON_NEGATIVE.fieldOf("offset_y").forGetter(ShieldBannerModifierSpriteSource::offsetY),
    NON_NEGATIVE.fieldOf("output_size").forGetter(ShieldBannerModifierSpriteSource::outSize)
  ).apply(inst, ShieldBannerModifierSpriteSource::new));
  /** Shield pattern textures, named by the banner pattern asset ID */
  private static final FileToIdConverter SHIELD_TEXTURES = new FileToIdConverter("textures/entity/shield", ".png");

  @Override
  public void run(ResourceManager manager, Output output) {
    // shield materials are created lazily since 1.20.5, so search the shield texture folder for patterns
    for (Entry<Identifier, Resource> entry : SHIELD_TEXTURES.listMatchingResources(manager).entrySet()) {
      Identifier input = entry.getKey();
      Identifier pattern = SHIELD_TEXTURES.fileToId(input);
      if (!pattern.getPath().startsWith("base")) {
        LazyLoadedImage image = new LazyLoadedImage(input, entry.getValue(), 1);
        Identifier destination = destinationPrefix.withSuffix(MaterialRenderInfo.getSuffix(pattern));
        output.add(destination, new BannerModifierSpriteSupplier(image, input, destination));
      }
    }
  }

  @Override
  public MapCodec<ShieldBannerModifierSpriteSource> codec() {
    return CODEC;
  }

  /** Generates a cropped sprite lazily */
  @RequiredArgsConstructor
  private class BannerModifierSpriteSupplier implements DiscardableLoader {
    private final LazyLoadedImage original;
    private final Identifier input, output;

    @Nullable
    @Override
    public SpriteContents get(SpriteResourceLoader loader) {
      try {
        // its possible the original is bigger than we expect due to HD pack, if so scale it accordingly
        // we only support scaling if it is a multiple of width
        NativeImage original = this.original.get();
        int scale = original.getWidth() / 64;
        if (scale == 0) {
          TConstruct.LOG.warn("Unable to crop {} to produce {} as texture size is less than 64", input, output);
        } else {
          NativeImage generated = new NativeImage(outSize * scale, outSize * scale, true);
          original.copyRect(generated, cropX * scale, cropY * scale, offsetX * scale, offsetY * scale, cropWidth * scale, cropHeight * scale, false, false);
          return new SpriteContents(this.output, new FrameSize(generated.getWidth(), generated.getHeight()), generated);
        }
      } catch (IllegalArgumentException | IOException ex) {
        TConstruct.LOG.warn("Unable to crop {} to produce {}", this.input, this.output, ex);
      } finally {
        this.original.release();
      }
      return null;
    }

    @Override
    public void discard() {
      this.original.release();
    }
  }
}

"""Ports TinkerClient, ClientEventBase and the fluid client events to 1.21.11"""
from javaport import port

T = 'src/main/java/slimeknights/tconstruct/'

port(T + 'fluids/FluidClientEvents.java',
     pairs=[('''    ItemBlockRenderTypes.setRenderLayer(fluid.getStill(), RenderType.translucent());
    ItemBlockRenderTypes.setRenderLayer(fluid.getFlowing(), RenderType.translucent());''', '''    ItemBlockRenderTypes.setRenderLayer(fluid.getStill(), ChunkSectionLayer.TRANSLUCENT);
    ItemBlockRenderTypes.setRenderLayer(fluid.getFlowing(), ChunkSectionLayer.TRANSLUCENT);''')],
     imports=['net.minecraft.client.renderer.chunk.ChunkSectionLayer'])

# block item tints moved to the item definitions
port(T + 'common/ClientEventBase.java',
     methods=['protected static void registerBlockItemColorAlias(BlockColors blockColors, ItemColors itemColors, Block block)',
              'protected static void registerBlockItemColorAlias(BlockColors blockColors, ItemColors itemColors, Supplier<? extends Block> block)',
              'protected static <B extends Block> void registerBlockItemColorAlias('])

port(T + 'shared/TinkerClient.java',
     lines=['ArmorTextureSupplier.LOADER.register(', 'ModifierModel.LOADER.register(', '// armor textures', '// modifier models'],
     pairs=[('''    Consumer<RecipesUpdatedEvent> recipesUpdated = event -> RecipeCacheInvalidator.reload(true);''', '''    Consumer<RecipesReceivedEvent> recipesUpdated = event -> RecipeCacheInvalidator.reload(true);'''),
            ('''        TextureAtlasSprite texture = minecraft.getBlockRenderer().getBlockModelShaper().getTexture(state, minecraft.level, pos);
        // changed: shader using pos tex
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // change: handle brightness based on renderWater, and enable blend
        Player player = minecraft.player;
        BlockPos blockpos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        float brightness = LightTexture.getBrightness(player.level().dimensionType(), player.level().getMaxLocalRawBrightness(blockpos));
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0f);
''', '''        TextureAtlasSprite texture = minecraft.getModelManager().getBlockModelShaper().getBlockModel(state).particleIcon(minecraft.level, pos, state);

        // change: handle brightness based on renderWater, and blend with the screen effect render type
        Player player = minecraft.player;
        BlockPos blockpos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        float brightness = LightTexture.getBrightness(player.level().dimensionType(), player.level().getMaxLocalRawBrightness(blockpos));
        int color = ARGB.colorFromFloat(1.0f, brightness, brightness, brightness);
        VertexConsumer bufferbuilder = event.getBufferSource().getBuffer(RenderTypes.blockScreenEffect(texture.atlasLocation()));
'''),
            ('''        // change: dropped color, see above
        bufferbuilder.addVertex(matrix4f, -1, -1, -0.5f).setUv(u1, v1);
        bufferbuilder.addVertex(matrix4f, 1, -1, -0.5f).setUv(u0, v1);
        bufferbuilder.addVertex(matrix4f, 1, 1, -0.5f).setUv(u0, v0);
        bufferbuilder.addVertex(matrix4f, -1, 1, -0.5f).setUv(u1, v0);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        // changed: disable blend''', '''        bufferbuilder.addVertex(matrix4f, -1, -1, -0.5f).setUv(u1, v1).setColor(color);
        bufferbuilder.addVertex(matrix4f, 1, -1, -0.5f).setUv(u0, v1).setColor(color);
        bufferbuilder.addVertex(matrix4f, 1, 1, -0.5f).setUv(u0, v0).setColor(color);
        bufferbuilder.addVertex(matrix4f, -1, 1, -0.5f).setUv(u1, v0).setColor(color);''')],
     imports=['net.neoforged.neoforge.client.event.RecipesReceivedEvent', 'com.mojang.blaze3d.vertex.VertexConsumer', 'net.minecraft.util.ARGB', 'net.minecraft.client.renderer.rendertype.RenderTypes'])

# JSON Things has no 1.21.11 client API in the build
port(T + 'shared/client/TConstructClient.java',
     pairs=[('''    if (ModList.get().isLoaded("jsonthings")) {
      PluginClient.init();
    }
''', '''    // shortcut: JSON Things tool models are not registered, PluginClient waits for a JSON Things build for 1.21.11
''')])
print('ok 3')

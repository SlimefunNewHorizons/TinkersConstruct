"""Ports the modifier client events to 1.21.11"""
from javaport import port

T = 'src/main/java/slimeknights/tconstruct/'

port(T + 'tools/client/ModifierClientEvents.java',
     pairs=[('renderPlayerArm(matrices, event.getMultiBufferSource(), event.getPackedLight(),', 'renderPlayerArm(matrices, event.getSubmitNodeCollector(), event.getPackedLight(),'),
            ('''        // draw background
        int light = 0xF000F0;
        MultiBufferSource buffer = graphics.bufferSource();
        VertexConsumer consumer = buffer.getBuffer(data == null ? ItemInHandRenderer.MAP_BACKGROUND : ItemInHandRenderer.MAP_BACKGROUND_CHECKERBOARD);
        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix,  -7, 135, 0).setColor(-1).setUv(0, 1).setLight(light);
        consumer.addVertex(matrix, 135, 135, 0).setColor(-1).setUv(1, 1).setLight(light);
        consumer.addVertex(matrix, 135,  -7, 0).setColor(-1).setUv(1, 0).setLight(light);
        consumer.addVertex(matrix,  -7,  -7, 0).setColor(-1).setUv(0, 0).setLight(light);

        // draw map if present
        if (data != null && index != null) {
          Minecraft.getInstance().gameRenderer.getMapRenderer().render(poseStack, buffer, index, data, false, light);
        }''', '''        // draw background
        Identifier background = data == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, -7, -7, 0, 0, 142, 142, 142, 142);

        // draw map if present
        if (data != null && index != null) {
          MapRenderState mapState = new MapRenderState();
          Minecraft.getInstance().getMapRenderer().extractRenderState(index, data, mapState);
          graphics.submitMapRenderState(mapState);
        }'''),
            ('public class ModifierClientEvents {', '''public class ModifierClientEvents {
  private static final Identifier MAP_BACKGROUND = Identifier.withDefaultNamespace("textures/map/map_background.png");
  private static final Identifier MAP_BACKGROUND_CHECKERBOARD = Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png");''')],
     imports=['net.minecraft.client.renderer.state.MapRenderState', 'net.minecraft.resources.Identifier', 'net.minecraft.client.renderer.RenderPipelines'])
print('ok 4')

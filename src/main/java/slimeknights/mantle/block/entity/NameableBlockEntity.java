package slimeknights.mantle.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.util.ValueIOHelper;

/**
 * Extension of tile entity to make it namable
 */
public abstract class NameableBlockEntity extends MantleBlockEntity implements INameableMenuProvider {
	private static final String TAG_CUSTOM_NAME = "CustomName";

	/** Default title for this tile entity */
	@Getter
	private final Component defaultName;
	/** Title set to this tile entity */
	@Getter @Setter
	private Component customName;

	public NameableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component defaultTitle) {
		super(type, pos, state);
		this.defaultName = defaultTitle;
	}

	@Override
	protected void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
		super.loadAdditional(tags, registries);
		if (tags.getString(TAG_CUSTOM_NAME).isPresent()) {
			this.customName = Component.Serializer.fromJson(tags.getString(TAG_CUSTOM_NAME), registries);
		}
	}

	@Override
	public void saveSynced(CompoundTag tags) {
		super.saveSynced(tags);
		if (this.hasCustomName()) {
			tags.put(TAG_CUSTOM_NAME, ValueIOHelper.saveComponent(this.customName, registries()));
		}
	}

	/** Returns the level registry lookup used for serializing synced component text. */
	private HolderLookup.Provider registries() {
		if (level == null) {
			throw new IllegalStateException("Cannot serialize a block entity name before it has a level");
		}
		return level.registryAccess();
	}
}

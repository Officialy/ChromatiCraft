package reika.chromaticraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only ChromatiCraft settings (config/chromaticraft-client.toml). */
public final class ChromaClientConfig {

	public static final ModConfigSpec SPEC;
	/** V33a's "ChromatiCraft Music" sound category (CHROMA_MUSIC): the Proxima soundtrack's own volume. */
	public static final ModConfigSpec.DoubleValue MUSIC_VOLUME;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		MUSIC_VOLUME = builder
				.comment("Volume of ChromatiCraft's music (the Proxima soundtrack), independent of the vanilla Music slider,",
						"as V33a's separate \"ChromatiCraft Music\" sound category was. Also set from Music & Sounds.")
				.defineInRange("chromaMusicVolume", 1.0, 0.0, 1.0);
		SPEC = builder.build();
	}

	private ChromaClientConfig() {}
}

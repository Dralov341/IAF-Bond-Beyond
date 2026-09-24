package com.iceandfirebondbeyond.util;

import com.google.gson.JsonParser;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = IceAndFireBondBeyond.MOD_ID)
public final class GuideBooks {
    private static final String GIVEN = "BondBeyondGuideGiven";
    private static com.google.gson.JsonArray readData(String name) {
        String path = "/assets/iceandfire_bond_beyond/books/" + name + ".json";
        try (InputStream stream = GuideBooks.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read Bond Beyond guide", exception);
        }
    }
    private static List<String> readPages(String language, boolean welcome) {
        List<String> pages = new ArrayList<>();
        for (var value : readData("guide_" + language)) pages.add(value.getAsString());
        if (welcome) {
            pages.remove(0);
            int index = 0;
            for (var value : readData("welcome_" + language)) pages.add(index++, value.getAsString());
        }
        return pages;
    }
    public static ItemStack create() {
        return create(true);
    }
    private static ItemStack create(boolean welcome) {
        List<String> vietnamese = readPages("vi_vn", welcome), english = readPages("en_us", welcome);
        ListTag pages = new ListTag();
        MutableComponent menu = Component.literal("BOND BEYOND\nGravtian\n\n")
                .append(link("Tiếng Việt →\n\n", 2))
                .append(link("English →\n\n", 3 + vietnamese.size()))
                .append(Component.literal("Minecraft 1.20.1\nForge · IAF beta-5"));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(menu)));
        appendLanguage(pages, vietnamese, "vi_vn", 2, welcome);
        appendLanguage(pages, english, "en_us", 3 + vietnamese.size(), welcome);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Bond Beyond — Sổ tay"); tag.putString("author", "Gravtian");
        // Vanilla BookCloningRecipe refuses generation 2; the gift has no recipe.
        tag.putInt("generation", 2); tag.put("pages", pages);
        tag.putBoolean("BondBeyondGuide", true);
        tag.putBoolean("BondBeyondWelcome", welcome);
        // Leave unresolved so vanilla synchronizes the book before opening it.
        return book;
    }
    private static void appendLanguage(ListTag pages, List<String> content, String language, int start, boolean welcome) {
        boolean vi = language.equals("vi_vn");
        int introExtra = welcome ? readData("welcome_" + language).size() - 1 : 0;
        for (int i = 0; i < content.size(); i++) {
            MutableComponent text = Component.literal(content.get(i) + "\n\n")
                    .append(link(vi ? "← Mục lục" : "← Contents", start + 1));
            pages.add(StringTag.valueOf(Component.Serializer.toJson(text)));
            if (i == 0) {
                MutableComponent contents = Component.literal(vi ? "MỤC LỤC\n\n" : "CONTENTS\n\n");
                for (var value : readData("toc_" + language)) {
                    var entry = value.getAsJsonObject();
                    int index = entry.get("page").getAsInt();
                    String label = entry.get("label").getAsString();
                    if (welcome && index == 0) label = vi ? "Lời chào / tác giả" : "Welcome / author";
                    if (index > 0) index += introExtra;
                    contents.append(link(label + "\n", start + index + (index == 0 ? 0 : 1)));
                }
                contents.append(Component.literal("\n")).append(link("Việt / English", 1));
                pages.add(StringTag.valueOf(Component.Serializer.toJson(contents)));
            }
        }
    }
    private static MutableComponent link(String text, int page) {
        return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.DARK_AQUA)
                .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Integer.toString(page))));
    }
    @SubscribeEvent public static void firstLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Existing gifted books keep their pages, but cannot be copied either.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing.is(Items.WRITTEN_BOOK) && existing.hasTag()
                    && existing.getTag().getBoolean("BondBeyondGuide")) {
                existing.getTag().putInt("generation", 2);
            }
        }
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persistent.getBoolean(GIVEN)) return;
        ItemStack book = create();
        if (!player.getInventory().add(book) && player.drop(book, false) == null) return;
        persistent.putBoolean(GIVEN, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
    }
    @SubscribeEvent public static void keepReceipt(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (old.getBoolean(GIVEN)) {
            CompoundTag persistent = event.getEntity().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            persistent.putBoolean(GIVEN, true);
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
        }
    }
    private GuideBooks() {}
}

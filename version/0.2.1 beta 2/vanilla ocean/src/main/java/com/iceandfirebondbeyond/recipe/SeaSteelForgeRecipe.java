package com.iceandfirebondbeyond.recipe;

import com.google.gson.JsonObject;
import com.iceandfirebondbeyond.IceAndFireBondBeyond;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public record SeaSteelForgeRecipe(ResourceLocation id, Ingredient input, Ingredient blood, ItemStack result, int cookTime) implements Recipe<Container> {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, IceAndFireBondBeyond.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, IceAndFireBondBeyond.MOD_ID);
    public static final RegistryObject<RecipeType<SeaSteelForgeRecipe>> TYPE = TYPES.register("sea_serpent_forge", () -> new RecipeType<>() {
        @Override public String toString() { return "iceandfire_bond_beyond:sea_serpent_forge"; }
    });
    public static final RegistryObject<RecipeSerializer<SeaSteelForgeRecipe>> SERIALIZER = SERIALIZERS.register("sea_serpent_forge", Serializer::new);
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
    @Override public boolean matches(Container container, Level level) { return input.test(container.getItem(0)) && blood.test(container.getItem(1)); }
    @Override public ItemStack assemble(Container container, RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input, blood); }
    @Override public boolean isSpecial() { return true; }
    private static final class Serializer implements RecipeSerializer<SeaSteelForgeRecipe> {
        @Override public SeaSteelForgeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new SeaSteelForgeRecipe(id, Ingredient.fromJson(json.get("input")), Ingredient.fromJson(json.get("blood")),
                    ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")),
                    Math.max(1, Math.min(72000, GsonHelper.getAsInt(json, "cook_time", 200))));
        }
        @Override public SeaSteelForgeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SeaSteelForgeRecipe(id, Ingredient.fromNetwork(buffer), Ingredient.fromNetwork(buffer), buffer.readItem(), buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, SeaSteelForgeRecipe recipe) {
            recipe.input.toNetwork(buffer); recipe.blood.toNetwork(buffer); buffer.writeItem(recipe.result); buffer.writeVarInt(recipe.cookTime);
        }
    }
}

package com.elytradev.teckle.api.capabilities;

import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CapabilityWorldNetworkAssistant {
    private static final ResourceLocation CAPABILITY_PROVIDER_NAME = new ResourceLocation(TeckleMod.MOD_ID, "NETWORK_ASSISTANT");
    @CapabilityInject(IWorldNetworkAssistant.class)
    public static Capability<IWorldNetworkAssistant> NETWORK_ASSISTANT_CAPABILITY = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(IWorldNetworkAssistant.class, new Capability.IStorage<IWorldNetworkAssistant>() {
                    @Override
                    public NBTBase writeNBT(Capability<IWorldNetworkAssistant> capability, IWorldNetworkAssistant instance, EnumFacing side) {
                        return new NBTTagCompound();
                    }

                    @Override
                    public void readNBT(Capability<IWorldNetworkAssistant> capability, IWorldNetworkAssistant instance, EnumFacing side, NBTBase base) {

                    }
                },
                () -> new IWorldNetworkAssistant() {
                });
        MinecraftForge.EVENT_BUS.register(CapabilityWorldNetworkAssistant.class);
    }

    @SuppressWarnings({"deprecation", "unused"}) // forge docs specify using this instead...
    public static void attachWorldEvent(AttachCapabilitiesEvent.World e) {
        e.addCapability(CAPABILITY_PROVIDER_NAME, new ICapabilityProvider() {
            public IWorldNetworkAssistant assistant;

            @Override
            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                if (capability == null)
                    return false;
                if (capability == NETWORK_ASSISTANT_CAPABILITY)
                    return true;

                return false;
            }

            @Nullable
            @Override
            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                if (capability == null)
                    return null;
                if (capability == NETWORK_ASSISTANT_CAPABILITY)
                    return (T) assistant;

                return null;
            }
        });
    }

}
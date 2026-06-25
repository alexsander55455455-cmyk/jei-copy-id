package com.jeicopyid;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Jeicopyid.MODID,
        name = Jeicopyid.NAME,
        version = Jeicopyid.VERSION,
        dependencies = "required-after:jei"
)
public class Jeicopyid {

    public static final String MODID = "jeicopyid";
    public static final String NAME = "JEI Copy ID";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} loaded", NAME);
    }
}
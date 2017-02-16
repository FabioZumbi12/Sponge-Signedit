package br.net.fabiozumbi12.signeditor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import com.google.inject.Inject;

@Plugin(id = "signeditor", 
name = "SignEditor", 
version = "1.0.3",
authors="FabioZumbi12", 
description="Simple tool to edit sign lines.")
public class SignEditor {
	
	private HashMap<String, List<Text>> lines = new HashMap<String, List<Text>>();
	private HashMap<String, SignData> signs = new HashMap<String, SignData>();
	private HashMap<String, Integer> copies = new HashMap<String, Integer>();

	@Inject
	Logger logger;
	
	
	@Listener
    public void onServerStart(GameStartedServerEvent event) {
		
		//help
		CommandSpec base = CommandSpec.builder()
			    .description(Text.of("Main command for SignEdit."))
			    .arguments(GenericArguments.optional(GenericArguments.string(Text.of("?"))))
			    .executor((src, args) -> {
			    	src.sendMessage(toText("&b---------------- SignEditor v1.0.3 ----------------"));
			    	src.sendMessage(toText("&bDeveloped by FabioZumbi12."));
			    	src.sendMessage(toText("&bFor more information about the commands, type [&6/signedit ?&b]."));
			    	src.sendMessage(toText("&b---------------------------------------------------"));
			    	
			    	if (args.getOne("?").isPresent()){
			    		src.sendMessage(toText("&b---------------- SignEditor v1.0.3 ----------------"));
				    	src.sendMessage(toText("&aAvailable Commands:"));
				    	src.sendMessage(toText("&8/setline <1-4> <text (max. length 16)> &r- &cEdit a sign line with colors."));
				    	src.sendMessage(toText("&8/copysign <copies> &r- &cCopy one sign text to other sign."));
				    	src.sendMessage(toText("&b---------------------------------------------------"));
			    	}
			    	return CommandResult.success();
				}).build();
		Sponge.getCommandManager().register(this, base, "signedit");	
		
		//setline
		CommandSpec setline = CommandSpec.builder()
			    .description(Text.of("Use to edit one line of a placed sign."))
			    .permission("signeditor.setline")
			    .arguments(GenericArguments.integer(Text.of("1-4")),GenericArguments.remainingJoinedStrings(Text.of("text (max. length 16)")))
			    .executor((src, args) -> { {
						if (src instanceof Player){
							int line = args.<Integer>getOne("1-4").get();
							String text = args.<String>getOne("text (max. length 16)").get();
							if (line > 4 || line < 1){
								throw new CommandException(toText("[SignEditor] &4The line need to be below 1-4."));
							}
							if (toText(text).toPlain().length() > 16){
								throw new CommandException(toText("[SignEditor] &4The max length allowed is 16 characters."));
							}
							
							List<Text> lineList = Arrays.asList(Text.of(),Text.of(),Text.of(),Text.of());
							if (lines.containsKey(src.getName())){
								lineList = lines.get(src.getName());
							}
							lineList.set(line-1, toText(text));							
							lines.put(src.getName(), lineList);
							src.sendMessage(toText("[SignEditor] &aLine '"+line+"' set to '&r"+text+"&a'. Click on sign to paste!"));
							return CommandResult.success();
						} 
						throw new CommandException(toText("[SignEditor] Only players can use this command."));
					}			    	
			    })
			    .build();
		Sponge.getCommandManager().register(this, setline, "setline");
		
		//copy
		CommandSpec copysign = CommandSpec.builder()
			    .description(Text.of("Copy one sign text to other signs"))
			    .permission("signeditor.copysign")
			    .arguments(GenericArguments.optional(GenericArguments.integer(Text.of("copies"))))
			    .executor((src, args) -> {
						if (src instanceof Player){
							signs.put(src.getName(), null);
							if (args.hasAny("copies") && args.<Integer>getOne("copies").get() > 1){
								int cop = args.<Integer>getOne("copies").get();
								copies.put(src.getName(), cop-1);
								src.sendMessage(toText("[SignEditor] &aClick in one sign to copy the text. ("+cop+" copies)"));
							} else {
								src.sendMessage(toText("[SignEditor] &aClick in one sign to copy the text. (no copies)"));
							}							
							return CommandResult.success();
						}
						throw new CommandException(toText("[SignEditor] Only players can use this command."));
					})
			    .build();
		Sponge.getCommandManager().register(this, copysign, "copysign");		
		
		//done
		logger.info(toColor("&aSignEditor enabled!"));
	}
	
	@Listener
	public void onStopServer(GameStoppingServerEvent e) {
		logger.info(toColor("&4SignEditor disabled!"));
	}
	
	@Listener
    public void onInteractBlock(InteractBlockEvent event, @First Player p) {
		String type = event.getTargetBlock().getState().getType().getName();
		if (type.endsWith("_sign")){
			if (lines.containsKey(p.getName())){				
				TileEntity signEntity = event.getTargetBlock().getLocation().get().getTileEntity().get();
				SignData data = signEntity.get(SignData.class).get();
				
				int linex = -1;
				List<Text> lineTexts = lines.get(p.getName());				
				for (Text l:lineTexts){
					linex++;
					if (l.isEmpty()){
						continue;
					}
					data.setElement(linex, l);
				}
				signEntity.offer(data);
				lines.remove(p.getName());
				event.setCancelled(true);
			} else if (signs.containsKey(p.getName())){
				if (signs.get(p.getName()) == null){
					signs.put(p.getName(), event.getTargetBlock().getLocation().get().getTileEntity().get().get(SignData.class).get());
					p.sendMessage(toText("[SignEditor] &aSign copied! Now click in other sign to paste the text."));
				} else {
					TileEntity signEntity = event.getTargetBlock().getLocation().get().getTileEntity().get();
					signEntity.offer(signs.get(p.getName()));
					
					if (copies.containsKey(p.getName())){
						int qtd = copies.get(p.getName());
						if (qtd <= 1){
							copies.remove(p.getName());
						} else {
							copies.put(p.getName(), qtd-1);
						}	
						p.sendMessage(toText("[SignEditor] &aText pasted with sucess! Still "+qtd+" copies to paste."));
					} else {
						signs.remove(p.getName());
						p.sendMessage(toText("[SignEditor] &aText pasted with sucess! (no copies)"));
					}
					
				}
				event.setCancelled(true);
			}
		}		
	}
	
	public static Text toText(String str){
    	return TextSerializers.FORMATTING_CODE.deserialize(str);
    }
	
	public static String toColor(String str){
    	return str.replaceAll("(&([a-fk-or0-9]))", "\u00A7$2"); 
    }
}
package gnomeshopping;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import javax.print.DocFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class gnome {
    public static void main(String[] args) throws IOException, InterruptedException {
        InputStream input = gnome.class.getClassLoader().getResourceAsStream("config.properties");
        Properties prop = PropertiesController.readPropertiesFile(input);
        if (prop.getProperty("token").equals("INSERT_TOKEN")) {
            System.out.print("You need to setup the bot in order to use it.");
            System.exit(0);
        }
        JDA api = JDABuilder.createDefault(prop.getProperty("token"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new gnomebot())
                .build().awaitReady();

        CommandListUpdateAction commands = api.updateCommands();
        commands.addCommands(Commands.slash("price", "Get the current price of an item")
                .setContexts(InteractionContextType.ALL)
                .setIntegrationTypes(IntegrationType.ALL)
                .addOption(OptionType.STRING, "query", "Item to lookup", true));
        commands.queue();
    }
}

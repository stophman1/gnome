package gnomeshopping;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class gnomebot extends ListenerAdapter {
    InputStream input = gnome.class.getClassLoader().getResourceAsStream("config.properties");


    public gnomebot() throws IOException {
    }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            switch (event.getName()) {
                case "price":
                    try {
                        price(event, event.getOption("query").getAsString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
            }
        }

        public static String formatCurrency(double totalGold) {
            long totalCopper = Math.round(totalGold * 10000);
            long gold = totalCopper / 10000;
            long remainingCopper = totalCopper % 10000;

            long silver = remainingCopper / 100;
            long copper = remainingCopper % 100;

            return String.format("%dg %ds %dc", gold, silver, copper);
        }

        public void price(SlashCommandInteractionEvent event, String query) throws IOException, InterruptedException {
            //todo handle sentinels and make it look all fancy
            event.deferReply().queue();
            wowapi wow = new wowapi();
            wowapi.Prices price = wow.getPrice(query);
            if (price.commodity() && ((price.allyquant() == -1) && (price.nquant() == -1))) {
                event.getHook().sendMessage("Couldn't find your item bub.").queue();
                return;
            }

            event.getHook().sendMessage(String.format("Yo yo yo here is the price of the %s: %s we got %s. Netural price: %s, %s of them",
                    wow.getItemName(query),formatCurrency(price.allyPrice()), price.allyquant(), formatCurrency(price.nPrice()), price.nquant())).queue();
        }



        @Override
        public void onMessageReceived(MessageReceivedEvent event)
        {
            if (event.getAuthor().isBot()) return;
            // We don't want to respond to other bot accounts, including ourself
            Message message = event.getMessage();
            String content = message.getContentRaw();
            // getContentRaw() is an atomic getter
            // getContentDisplay() is a lazy getter which modifies the content for e.g. console view (strip discord formatting)
            if (content.equals("!ping"))
            {
                MessageChannel channel = event.getChannel();
                channel.sendMessage("Pong!").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
            }
        }

}

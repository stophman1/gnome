package gnomeshopping;

import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.section.SectionContentComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class gnomebot extends ListenerAdapter {
    public gnomebot()  {
    }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            switch (event.getName()) {
                case "price":
                    try {
                        OptionMapping query = event.getOption("query");
                        assert query != null;
                        price(event, query.getAsString());
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                case "joe" : 
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
            if (price.itemID() == -1) {
                event.getHook().sendMessage("Couldn't find your item bub.").queue();
                return;
            }
            wowapi.itemData name = wow.getItemName(price.itemID());

            List<SectionContentComponent> child = new ArrayList<>();
            child.add(TextDisplay.of(name.quality()));
            if (price.allyPrice() != 9.223372036854776E14) {
                child.add(TextDisplay.of(String.format("Alliance AH: %s Total at Price: %s", formatCurrency(price.allyPrice()), price.allyquant())));
            }
            if (price.nPrice() != 9.223372036854776E14) {
                child.add(TextDisplay.of(String.format("Neutral AH: %s Total at Price: %s", formatCurrency(price.nPrice()), price.nquant())));
            }

            Container container = Container.of(
                        TextDisplay.of("## [" + name.name()+"](https://www.wowhead.com/forever/item="+ price.itemID()+")"),
                        Section.of(
                                Thumbnail.fromUrl(name.icon()).withDescription(name.name()),
                                child
                        ),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TextDisplay.of("-# V0.1")
                ).withAccentColor(new Color(getColor(name.quality())));


            //event.getHook().sendMessage(String.format("Yo yo yo here is the price of the %s: %s we got %s. Neutral price: %s, %s of them",
            //        name.name(),formatCurrency(price.allyPrice()), price.allyquant(), formatCurrency(price.nPrice()), price.nquant())).queue();
            event.getHook().sendMessageComponents(container).useComponentsV2().queue();
        }

        private int getColor(String quality) {
            return switch (quality) {
                case "Common" -> 16777215;
                case "Uncommon" -> 2031360;
                case "Rare" -> 28893;
                case "Epic" -> 10696174;
                case "Legendary" -> 16744448;
                case "Artifact" -> 15125632;
                case "Heirloom" -> 52479;
                default -> 16777215;
            };
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

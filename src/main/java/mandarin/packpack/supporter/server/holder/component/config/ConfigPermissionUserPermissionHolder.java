package mandarin.packpack.supporter.server.holder.component.config;

import common.CommonStatic;
import kotlin.Pair;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ConfigPermissionUserPermissionHolder extends ServerConfigHolder {
    private static final int PAGE_CHUNK = 10;

    private final String userID;

    private Member member;

    private final List<Pair<String, String>> adjustableChannelPermission;

    private int page = 0;

    public ConfigPermissionUserPermissionHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, @NotNull Guild g, String userID, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, holder, backup, lang);

        this.userID = userID;

        CountDownLatch countdown = new CountDownLatch(1);

        try {
            g.retrieveMemberById(userID).queue(m -> {
                member = m;

                countdown.countDown();
            }, e -> {
                StaticStore.logger.uploadErrorLog(e, "E/ConfigPermissionUserPermissionHolder::init - Failed to get member %s".formatted(userID));

                member = null;

                countdown.countDown();
            });

            countdown.await();
        } catch (InterruptedException e) {
            StaticStore.logger.uploadErrorLog(e, "E/ConfigPermissionUserPermissionHolder::init - Failed to get member %s".formatted(userID));

            member = null;
        }

        adjustableChannelPermission = new ArrayList<>();

        filterActivatedChannelPermission();
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "adjust" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                String id = e.getValues().getFirst();

                List<String> deactivatedChannelPermission = holder.channelException.computeIfAbsent(userID, k -> new ArrayList<>());

                if (deactivatedChannelPermission.contains(id)) {
                    deactivatedChannelPermission.remove(id);
                } else {
                    deactivatedChannelPermission.add(id);
                }

                if (deactivatedChannelPermission.isEmpty())
                    holder.channelException.remove(userID);

                filterActivatedChannelPermission();

                applyResult(event);
            }
            case "prev10" -> {
                page -= 10;

                applyResult(event);
            }
            case "prev" -> {
                page--;

                applyResult(event);
            }
            case "next" -> {
                page++;

                applyResult(event);
            }
            case "next10" -> {
                page += 10;

                applyResult(event);
            }
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("sercon_done", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("sercon_cancelask", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("sercon_cancel", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    expired = true;
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void filterActivatedChannelPermission() {
        adjustableChannelPermission.clear();

        if (member == null)
            return;

        if (holder.MEMBER != null) {
            adjustableChannelPermission.add(new Pair<>("", "MEMBER|" + holder.MEMBER));
        }

        if (holder.BOOSTER != null) {
            adjustableChannelPermission.add(new Pair<>("", "BOOSTER|" + holder.BOOSTER));
        }

        for (String key : holder.ID.keySet()) {
            String id = holder.ID.get(key);

            if (id == null)
                continue;

            adjustableChannelPermission.add(new Pair<>(key, id));
        }

        List<String> roleID = member.getRoles().stream().map(Role::getId).toList();

        adjustableChannelPermission.removeIf((pair) -> {
            String id;

            if (pair.getSecond().startsWith("MEMBER|")) {
                id = pair.getSecond().replace("MEMBER|", "");
            } else if (pair.getSecond().startsWith("BOOSTER|")) {
                id = pair.getSecond().replace("BOOSTER|", "");
            } else {
                id = pair.getSecond();
            }

            return !roleID.contains(id);
        });
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        List<String> deactivatedChannelPermission = holder.channelException.get(userID);

        builder.append(LangID.getStringByID("sercon_permission", lang)).append("\n")
                .append(LangID.getStringByID("sercon_permissionmanage", lang).formatted(Emoji.fromUnicode("🔧"))).append("\n")
                .append(LangID.getStringByID("sercon_permissiondeactivatedesc", lang).formatted("<@" + userID + ">")).append("\n")
                .append(LangID.getStringByID("serconpermissiondeactivateadjustable", lang)).append("\n");

        if (adjustableChannelPermission.isEmpty()) {
            builder.append(LangID.getStringByID("sercon_permissiondeactivatenorole", lang));
        } else {
            int size = Math.min(adjustableChannelPermission.size(), (page + 1) * PAGE_CHUNK);

            for (int i = page * PAGE_CHUNK; i < size; i++) {
                String id = adjustableChannelPermission.get(i).getSecond();
                String name;

                boolean isCustom = false;

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");
                    name = LangID.getStringByID("sercon_permissionrolemember", lang);
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    name = LangID.getStringByID("sercon_permissionrolelbooster", lang);
                } else {
                    name = LangID.getStringByID("sercon_permissionrolecustom", lang).formatted(adjustableChannelPermission.get(i).getFirst());
                    isCustom = true;
                }

                boolean activated = deactivatedChannelPermission == null || !deactivatedChannelPermission.contains(id);

                String formattedEmoji;

                if (activated)
                    formattedEmoji = EmojiStore.SWITCHON.getFormatted();
                else
                    formattedEmoji = EmojiStore.SWITCHOFF.getFormatted();

                builder.append(i + 1).append(". ")
                        .append(formattedEmoji).append(" ")
                        .append(name).append("<@&").append(id).append("> [").append(id).append("]");

                if (isCustom) {
                    builder.append(" <").append(LangID.getStringByID("sercon_permissionrolecustomtype", lang)).append(">");
                }

                if (i < size - 1){
                    builder.append("\n");
                }
            }

            if (adjustableChannelPermission.size() > PAGE_CHUNK) {
                builder.append("\n").append(LangID.getStringByID("formst_page", lang).formatted(page + 1, getTotalPage(adjustableChannelPermission.size())));
            }
        }

        return builder.toString();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        List<String> deactivatedChannelPermission = holder.channelException.get(userID);

        List<SelectOption> activatedOption = new ArrayList<>();

        if (adjustableChannelPermission.isEmpty()) {
            activatedOption.add(SelectOption.of("A", "A"));
        } else {
            int size = Math.min(adjustableChannelPermission.size(), (page + 1) * PAGE_CHUNK);

            for (int i = page * PAGE_CHUNK; i < size; i++) {
                String id = adjustableChannelPermission.get(i).getSecond();
                String label;

                if (id.startsWith("MEMBER|")) {
                    id = id.replace("MEMBER|", "");
                    label = LangID.getStringByID("sercon_permissionrolemembertype", lang);
                } else if (id.startsWith("BOOSTER|")) {
                    id = id.replace("BOOSTER|", "");
                    label = LangID.getStringByID("sercon_permissionrolelboostertype", lang);
                } else {
                    label = adjustableChannelPermission.get(i).getFirst() + " <" + LangID.getStringByID("sercon_permissionrolecustomtype", lang) + ">";
                }

                boolean activated = deactivatedChannelPermission == null || !deactivatedChannelPermission.contains(id);

                Emoji emoji;

                if (activated)
                    emoji = EmojiStore.SWITCHON;
                else
                    emoji = EmojiStore.SWITCHOFF;

                activatedOption.add(SelectOption.of(label, id).withDescription(id).withEmoji(emoji));
            }
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("adjust")
                        .addOptions(activatedOption)
                        .setDisabled(adjustableChannelPermission.isEmpty())
                        .setPlaceholder(LangID.getStringByID("sercon_permissiondeactivateactivate", lang))
                        .setRequiredRange(1, 1)
                        .build()
        ));

        if (adjustableChannelPermission.size() > PAGE_CHUNK) {
            List<Button> buttons = new ArrayList<>();

            int totalPage = getTotalPage(adjustableChannelPermission.size());

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("search_prev10", lang), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("search_prev", lang), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("search_next", lang), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("search_next10", lang), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
            }

            result.add(ActionRow.of(buttons));
        }

        result.add(
                ActionRow.of(
                        Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                        Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                        Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                )
        );

        return result;
    }
}

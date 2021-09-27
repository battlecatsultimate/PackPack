package mandarin.packpack.supporter.server.holder;

import common.pack.Identifier;
import common.pack.UserProfile;
import common.util.pack.Background;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import common.util.stage.Music;
import common.util.stage.Stage;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.commands.bc.Castle;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.slash.WebhookBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class StageInfoButtonHolder extends InteractionHolder<ButtonInteractionEvent> {
    private final Message embed;
    private final String channelID;
    private final String memberID;
    private final Stage st;

    public StageInfoButtonHolder(Stage st, Message author, Message msg, String channelID, String memberID) {
        super(ButtonInteractionEvent.class);

        this.st = st;
        embed = msg;
        this.channelID = channelID;
        this.memberID = memberID;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), StageInfoButtonHolder.this));

                embed.removeAllReactions().subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(ButtonInteractionEvent event) {
        if(expired) {
            System.out.println("Expired at StageReactionHolder!");
            return RESULT_FAIL;
        }

        MessageChannel ch = embed.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID))
            return RESULT_STILL;

        if(event.getInteraction().getMember().isEmpty())
            return RESULT_STILL;

        Member mem = event.getInteraction().getMember().get();

        if(!mem.getId().asString().equals(memberID))
            return RESULT_STILL;

        if(event.getMessage().isEmpty())
            return RESULT_STILL;

        Message m = event.getMessage().get();

        if(!m.getId().asString().equals(embed.getId().asString()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public Mono<?> getInteraction(ButtonInteractionEvent event) {
        expire("");

        switch (event.getCustomId()) {
            case "music":
                if(st.mus0 == null)
                    return Mono.empty();

                Music ms = Identifier.get(st.mus0);

                if(ms == null) {
                    ms = UserProfile.getBCData().musics.get(0);
                }

                try {
                    WebhookBuilder request = mandarin.packpack.commands.bc.Music.getInteractionWebhook(event.getInteraction().getData(), ms);

                    if(request == null)
                        return Mono.empty();

                    return event.deferReply()
                            .then(event.getInteractionResponse().createFollowupMessage(request.build()))
                            .then(Mono.create(m -> request.finishJob(true)))
                            .doOnError(e -> {
                                e.printStackTrace();
                                StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to perform interaction for music");
                                request.finishJob(true);
                            });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for music");
                    e.printStackTrace();
                }
                break;
            case "music2":
                if(st.mus1 == null)
                    return Mono.empty();

                ms = Identifier.get(st.mus1);

                if(ms == null) {
                    ms = UserProfile.getBCData().musics.get(0);
                }

                try {
                    WebhookBuilder request = mandarin.packpack.commands.bc.Music.getInteractionWebhook(event.getInteraction().getData(), ms);

                    if(request == null)
                        return Mono.empty();

                    return event.deferReply()
                            .then(event.getInteractionResponse().createFollowupMessage(request.build()))
                            .then(Mono.create(m -> request.finishJob(true)))
                            .doOnError(e -> {
                                e.printStackTrace();
                                StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to perform interaction for music");
                                request.finishJob(true);
                            });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for music");
                    e.printStackTrace();
                }
                break;
            case "bg":
                Background bg = Identifier.get(st.bg);

                if(bg == null)
                    bg = UserProfile.getBCData().bgs.get(0);

                try {
                    WebhookBuilder request = mandarin.packpack.commands.bc.Background.getInteractionWebhook(event.getInteraction().getData(), bg);

                    if(request == null)
                        return Mono.empty();

                    return event.deferReply()
                            .then(event.getInteractionResponse().createFollowupMessage(request.build()))
                            .then(Mono.create(m -> request.finishJob(true)))
                            .doOnError(e -> {
                                e.printStackTrace();
                                StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to perform interaction for bg");
                                request.finishJob(true);
                            });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for bg");
                    e.printStackTrace();
                }
                break;
            case "castle":
                CastleImg cs = Identifier.get(st.castle);

                if(cs == null) {
                    ArrayList<CastleList> lists = new ArrayList<>(CastleList.defset());

                    cs = lists.get(0).get(0);
                }

                try {
                    WebhookBuilder request = Castle.getInteractionWebhook(event.getInteraction().getData(), cs);

                    if(request == null)
                        return Mono.empty();

                    return event.deferReply()
                            .then(event.getInteractionResponse().createFollowupMessage(request.build()))
                            .then(Mono.create(m -> request.finishJob(true)))
                            .doOnError(e -> {
                                e.printStackTrace();
                                StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to perform interaction for castle");
                                request.finishJob(true);
                            });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for castle");
                    e.printStackTrace();
                }
                break;
        }

        return Mono.empty();
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        Command.editMessage(embed, m -> {
            ArrayList<Button> buttons = new ArrayList<>();

            for(LayoutComponent layoutComponent : embed.getComponents()) {
                for(MessageComponent component : layoutComponent.getChildren()) {
                    if(component instanceof Button) {
                        if(((Button) component).getStyle() == Button.Style.SECONDARY) {
                            buttons.add(((Button) component).disabled(true));
                        } else {
                            buttons.add((Button) component);
                        }
                    }
                }
            }

            m.addComponent(ActionRow.of(buttons));
        });
    }
}

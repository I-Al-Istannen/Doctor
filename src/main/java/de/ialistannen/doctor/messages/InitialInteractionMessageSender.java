package de.ialistannen.doctor.messages;

import de.ialistannen.doctor.state.SentMessageHandle;
import de.ialistannen.doctor.state.SentMessageHandle.InteractionHookHandle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.ButtonInteraction;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.requests.RestAction;

public class InitialInteractionMessageSender implements MessageSender {

  private final Interaction interaction;

  public InitialInteractionMessageSender(Interaction interaction) {
    this.interaction = interaction;
  }

  @Override
  public RestAction<SentMessageHandle> reply(Message message) {
    return interaction.reply(message)
        .flatMap(hook -> hook.retrieveOriginal().map(it -> new InteractionHookHandle(hook, it)));
  }

  @Override
  public RestAction<SentMessageHandle> editOrReply(Message newMessage) {
    if (interaction instanceof ComponentInteraction) {
      return ((ComponentInteraction) interaction).editMessage(newMessage)
          .flatMap(hook -> hook.retrieveOriginal().map(it -> new InteractionHookHandle(hook, it)));
    }
    return reply(newMessage);
  }

  @Override
  public RestAction<Void> delete() {
    // We need to ack it before we can delete the original
    // deferReply adds a new reply and *that* would be deleted so we can't do that and need to use
    // deferEdit
    if (interaction instanceof ComponentInteraction) {
      ((ComponentInteraction) interaction).deferEdit().queue();
    }
    return interaction.getHook().deleteOriginal();
  }
}

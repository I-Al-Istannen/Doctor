package de.ialistannen.doctor.messages;

import de.ialistannen.doctor.state.SentMessageHandle;
import de.ialistannen.doctor.state.SentMessageHandle.InteractionHookHandle;
import de.ialistannen.doctor.state.SentMessageHandle.MessageHandle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;

public class InteractionHookMessageSender implements MessageSender {

  private final InteractionHook hook;

  public InteractionHookMessageSender(InteractionHook hook) {
    this.hook = hook;
  }

  @Override
  public RestAction<SentMessageHandle> reply(Message message) {
    if (!hook.isExpired()) {
      return hook.sendMessage(message).map(it -> new InteractionHookHandle(hook, it));
    }
    return hook.getInteraction().getTextChannel().sendMessage(message).map(MessageHandle::new);
  }

  @Override
  public RestAction<SentMessageHandle> editOrReply(Message newMessage) {
    if (!hook.isExpired()) {
      return hook.editOriginal(newMessage).map(it -> new InteractionHookHandle(hook, it));
    }
    return hook.retrieveOriginal()
        .flatMap(message -> message.editMessage(newMessage))
        .map(MessageHandle::new);
  }

  @Override
  public RestAction<Void> delete() {
    if (!hook.isExpired()) {
      return hook.deleteOriginal();
    }
    return hook.retrieveOriginal().flatMap(Message::delete);
  }
}

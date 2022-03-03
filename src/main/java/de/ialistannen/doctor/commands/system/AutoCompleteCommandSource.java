package de.ialistannen.doctor.commands.system;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

public class AutoCompleteCommandSource implements CommandSource {
    private final CommandAutoCompleteInteractionEvent event;

    public AutoCompleteCommandSource(CommandAutoCompleteInteractionEvent event) {
        this.event = event;
    }

    public CommandAutoCompleteInteractionEvent getEvent() {
        return event;
    }

    public String getOptionName() {
        return event.getFocusedOption().getName();
    }

    public String getOptionValue() {
        return event.getFocusedOption().getValue();
    }

    @Override
    public String getAuthorId() {
        return event.getInteraction().getUser().getId();
    }

    @Override
    public String getId() {
        return event.getId();
    }
}

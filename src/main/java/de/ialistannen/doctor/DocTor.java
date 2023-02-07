package de.ialistannen.doctor;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import de.ialistannen.doctor.command.CommandListener;
import de.ialistannen.doctor.command.DocCommand;
import de.ialistannen.doctor.storage.ActiveMessages;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class DocTor {

  public static void main(String[] args) throws IOException, InterruptedException, SQLException {
    TomlMapper mapper = TomlMapper.builder().build();

    DocTorConfig config = mapper.readValue(
        Files.readString(Path.of(args[0])),
        DocTorConfig.class
    );

    ActiveMessages activeMessages = new ActiveMessages();
    DocCommand docCommand = DocCommand.create(config, activeMessages);

    JDA jda = JDABuilder.createDefault(config.token())
        .addEventListeners(new CommandListener(config, docCommand, activeMessages))
        .build()
        .awaitReady();
    System.out.println(jda.getInviteUrl());
  }
}

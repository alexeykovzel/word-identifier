package com.alexeykovzel;

import com.vdurmont.emoji.EmojiParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class BotTranslate extends TelegramLongPollingBot {

    private final Properties prop = new Properties();
    private String lastWord;

    BotTranslate() {
        try (InputStream input = new FileInputStream("/home/aliakseik/Projects/word-identifier/src/main/resources/string.properties")) {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String messageText, long id) {
        String text = EmojiParser.parseToUnicode(messageText);
        SendMessage message = new SendMessage()
                .setChatId(id)
                .setText(text)
                .enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();
            String response;
            response = "";
            Document doc;
            if (message.charAt(0) == '/') {
                switch (message) {
                    case "/start":
                        sendMessage("Hello, " + update.getMessage().getChat().getUserName() + "!\n\nTo make use of a bot write any word you would like to find out :yum:", chatId);
                        sendMessage(prop.getProperty("help"), chatId);
                        break;
                    case "/examples":
                        try {
                            doc = Jsoup.connect("https://www.collinsdictionary.com/dictionary/english/" + lastWord).get();
                            if (lastWord != null) {
                                int i = 1;
                                for (Element example : doc.body().getElementsByClass("cit")) {
                                    if (i <= 5) {
                                        response += "- " + example.text() + "\n\n";
                                        i++;
                                    } else {
                                        break;
                                    }
                                }
                                sendMessage(response, chatId);
                            } else {
                                sendMessage(prop.getProperty("error_examples"), chatId);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        sendMessage(prop.getProperty("error_command"), chatId);
                }
            } else {
                try {
                    doc = Jsoup.connect("https://www.collinsdictionary.com/dictionary/english/" + message).get();
                    try {
                        String gramGrp = doc.body().getElementsByClass("gramGrp pos").first().text();
                        if (gramGrp != null) {
                            lastWord = message;
                            response += "*" + gramGrp + "*\n\n";
                            int i = 1;
                            for (Element def : doc.body().getElementsByClass("def")) {
                                if (i <= 3) {
                                    response += "- " + def.text() + "\n\n";
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        }
                    } catch (NullPointerException e) {
                        response = prop.getProperty("error");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendMessage(response, chatId);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "@word_definition_bot";
    }

    @Override
    public String getBotToken() {
        return "1120057655:AAEco8Jumea0Xj8S3gOP2vKp9FIHkAY4S2o";
    }
}

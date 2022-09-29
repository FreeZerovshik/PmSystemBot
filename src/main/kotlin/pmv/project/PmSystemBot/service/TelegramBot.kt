package pmv.project.PmSystemBot.service

import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import pmv.project.PmSystemBot.config.BotConfig
import java.lang.RuntimeException

@Component
class TelegramBot: TelegramLongPollingBot {


    final var config: BotConfig

    constructor(botConfig: BotConfig){
        this.config = botConfig
    }


    override fun getBotToken(): String {
        return config.token
    }

    override fun getBotUsername(): String {
        return  config.botName
    }

    override fun onUpdateReceived(update: Update?) {
        if (update != null) {
            if (update.hasMessage() && update.message.hasText()) {
                val messageText = update.message.text
                val chatId= update.message.chatId

                when (messageText) {
                    "/start" -> startCommandReciver(chatId, update.message.chat.userName)
                else -> sendMessage(chatId, "Sorry, command was recognized!")
                }
            }
        }
    }

    private fun startCommandReciver(chatId: Long?, name: String) {
        val answer = "Hello ${name}, nice too met you!"

        sendMessage(chatId, answer)
    }

    private fun sendMessage(chatId: Long?, textToSend: String)  {
        val message = SendMessage()
        message.chatId = chatId.toString()
        message.text = textToSend

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            throw RuntimeException(e)
        }

    }

}
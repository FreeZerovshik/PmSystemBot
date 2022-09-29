package pmv.project.PmSystemBot.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.meta.generics.TelegramBot
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
class BotInit {

    @Autowired
    lateinit var bot: TelegramBot

    @EventListener(classes = [ContextRefreshedEvent::class])
    fun init()  {
        val telegramBotsApi: TelegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)

        try {
            telegramBotsApi.registerBot(bot as LongPollingBot?)
        } catch (_: TelegramApiException) {

        }
    }
}
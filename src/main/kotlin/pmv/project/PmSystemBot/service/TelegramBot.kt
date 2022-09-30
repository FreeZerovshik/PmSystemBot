package pmv.project.PmSystemBot.service


import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import pmv.project.PmSystemBot.config.BotConfig
import pmv.project.PmSystemBot.model.User
import pmv.project.PmSystemBot.model.UserRepository
import java.lang.RuntimeException
import java.sql.Timestamp

@Component
class TelegramBot(botConfig: BotConfig) : TelegramLongPollingBot() {
    private val logger = KotlinLogging.logger {}

    @Autowired
    lateinit var userRepository: UserRepository

    private final var config: BotConfig = botConfig

    private final val HELP_INFO = "Этот бот создан как пет проект, для изучения возможностей TelegramBotApi на языке разработки Kotlin\n" +
            "Введите /start для запуска бота\n" +
            "Введите /myinfo для получения информации о себе\n" +
            "Введите /deleteinfo для удаления инфомарции о себе\n" +
            "Введите /help для получения справки по работе данного бота\n"


    init {
        val commandsList = mutableListOf<BotCommand>()
        commandsList.add(BotCommand("/start", "Start Bot"))
        commandsList.add(BotCommand("/myinfo", "Show my info"))
        commandsList.add(BotCommand("/deleteinfo", "Delete my info"))
        commandsList.add(BotCommand("/help","Show help"))
        try {
            this.execute(SetMyCommands(commandsList, BotCommandScopeDefault(), null))
        } catch (e: TelegramApiException) {
            logger.error("Error: ${e.message}")
        }
    }


    override fun getBotToken(): String {
        return config.token
    }

    override fun getBotUsername(): String {
        return  config.botName
    }

    private fun registerUser(msg: Message) {

        val chat = msg.chat
        val chatId = chat.id

        if (userRepository.findById(chatId).isEmpty) {
            val user = User(
                id = chatId,
                firstName = chat.firstName,
                lastName = chat.lastName,
                userName = chat.userName,
                registeredAt = Timestamp(System.currentTimeMillis())
            )

            userRepository.save(user)
            startCommandReciver(chatId, chat.userName)
        } else {
            sendMessage(chatId, "User already register!")
        }
    }

    override fun onUpdateReceived(update: Update?) {
        if (update != null) {
            if (update.hasMessage() && update.message.hasText()) {
                val messageText = update.message.text
                val chatId= update.message.chatId

                when (messageText) {
                    "/start" -> registerUser(update.message)
                    "/myinfo" -> sendMessage(chatId, "Username: ${update.message.chat.userName} \n" +
                            "Firstname: ${update.message.chat.firstName} \n" +
                            "LastName: ${update.message.chat.lastName} \n" +
                            "Bio: ${update.message.chat.bio} \n" +
                            "Contacts: ${update.message.contact} \n" +
                            "Location: ${update.message.chat?.location?.address} \n" )
                    "/help" -> sendMessage(chatId, HELP_INFO)
                else -> sendMessage(chatId, "Sorry, command was recognized!")
                }
            }
        }
    }

    private fun startCommandReciver(chatId: Long?, name: String) {
        val answer = "Hello ${name}, nice too met you!"
        logger.info("Replied to user $name")
        sendMessage(chatId, answer)
    }

    private fun sendMessage(chatId: Long?, textToSend: String)  {
        val message = SendMessage()
        message.chatId = chatId.toString()
        message.text = textToSend

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            logger.error("Error: ${e.message}")
        }

    }

}
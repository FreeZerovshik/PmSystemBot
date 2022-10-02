package pmv.project.PmSystemBot.service


import com.vdurmont.emoji.EmojiParser
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import pmv.project.PmSystemBot.config.BotConfig
import pmv.project.PmSystemBot.constant.Constant
import pmv.project.PmSystemBot.model.User
import pmv.project.PmSystemBot.model.UserRepository
import java.sql.Timestamp

private const val ERROR_MSG = "Error:"

@Component
class TelegramBot(botConfig: BotConfig) : TelegramLongPollingBot() {
    private val logger = KotlinLogging.logger {}

    @Autowired
    lateinit var userRepository: UserRepository

    private final var config: BotConfig = botConfig

    init {
        val commandsList = mutableListOf<BotCommand>()
        commandsList.add(BotCommand("/start", "Регистрация"))
        commandsList.add(BotCommand("/myinfo", "Показать мой профиль"))
        commandsList.add(BotCommand("/deleteinfo", "Удалить мой профиль и выйти"))
        commandsList.add(BotCommand("/mytasks", "Показать мои задачи"))
        commandsList.add(BotCommand("/gettask", "Показать задачу"))
        commandsList.add(BotCommand("/help", "Справка"))
        try {
            this.execute(SetMyCommands(commandsList, BotCommandScopeDefault(), null))
        } catch (e: TelegramApiException) {
            logger.error("$ERROR_MSG ${e.message}")
        }
    }


    override fun getBotToken(): String {
        return config.token
    }

    override fun getBotUsername(): String {
        return config.botName
    }


    private fun getKeyboard(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup()

        val keyboardRows = mutableListOf<KeyboardRow>()

        val row = KeyboardRow()
        row.add("текущие задачи")
        row.add("отметить часы по задаче")
        keyboardRows.add(row)

        keyboardMarkup.keyboard = keyboardRows

        return keyboardMarkup
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
            sendMessage(chatId, EmojiParser.parseToUnicode("User already register! :sweat_smile:"))
        }
    }

    private fun getMyInfo(chatId: Long, message: Message) {
        sendMessage(
            chatId, "Username: ${message.chat.userName} \n" +
                    "Firstname: ${message.chat.firstName} \n" +
                    "LastName: ${message.chat.lastName} \n" +
                    "Bio: ${message.chat.bio} \n" +
                    "Contacts: ${message.contact} \n" +
                    "Location: ${message.chat?.location?.address} \n"
        )
    }

    override fun onUpdateReceived(update: Update?) {
        if (update != null) {
            if (update.hasMessage() && update.message.hasText()) {
                val messageText = update.message.text
                val chatId = update.message.chatId

                //массовая рассылка пользователям если команда содержит /send и всем оптраялется текст после пробела (доступна только пладельцу бота)
                if (messageText.contains("/send") && config.owner == chatId) {
                    val textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")))

                    userRepository.findAll().forEach {
                        sendMessage(it.id, textToSend)
                    }
                } else {

                    //обработка команд 
                    when (messageText) {
                        "/start" -> registerUser(update.message)
                        "/myinfo" -> getMyInfo(chatId, update.message)
                        "/mytasks" -> getMyTasks(chatId)
                        "/gettask" -> getTask(chatId)
                        "/help" -> sendMessage(chatId, Constant.HELP_INFO)
                        else -> sendMessage(chatId, "Sorry, command was recognized!")
                    }
                }
            } else if (update.hasCallbackQuery()) {
                // Обработка ответов на пункты меню
                val callbackData = update.callbackQuery.data
                val messageId = update.callbackQuery.message.messageId
                val chatId = update.callbackQuery.message.chatId

                if (callbackData.equals(Constant.STATUS_BUTTON)) {
                    val text = "Статус по задаче изменен на ... "
                    executeEditMessageText(chatId, text, messageId)
                } else if (callbackData.equals(Constant.DATEEND_BUTTON)) {
                    val text = "Дата окончания изменена на ..."
                    executeEditMessageText(chatId, text, messageId)
                } else if (callbackData.equals(Constant.ASSIGNEE_BUTTON)) {
                    val text = "Исполнитель изменен на ..."
                    executeEditMessageText(chatId, text, messageId)
                }

            }
        }
    }

    private fun executeEditMessageText(chatId: Long, text: String, messageId: Int?) {
        val message = EditMessageText()
        message.chatId = chatId.toString()
        message.text = text
        message.messageId = messageId

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            logger.error("$ERROR_MSG ${e.message}")
        }
    }

    private fun getTask(chatId: Long) {

        val message = SendMessage()
        message.chatId = chatId.toString()
        message.text = "Введите номер задачи"

        val inlineMarkup = InlineKeyboardMarkup()
        val rowsInline = mutableListOf<MutableList<InlineKeyboardButton>>()
        val rowInLine = mutableListOf<InlineKeyboardButton>()
        val statusButton = InlineKeyboardButton()
        statusButton.text = "Сменить статус"
        statusButton.callbackData = Constant.STATUS_BUTTON

        val dateEndButton = InlineKeyboardButton()
        dateEndButton.text = "Изменить дату окончания"
        dateEndButton.callbackData = Constant.DATEEND_BUTTON

        val assigneeButton = InlineKeyboardButton()
        assigneeButton.text = "Поменять исполнителя"
        assigneeButton.callbackData = Constant.ASSIGNEE_BUTTON

        rowInLine.add(statusButton)
        rowInLine.add(dateEndButton)
        rowInLine.add(assigneeButton)

        rowsInline.add(rowInLine)

        inlineMarkup.keyboard = rowsInline
        message.replyMarkup = inlineMarkup

        executeMessage(message)
    }

    private fun getMyTasks(chatId: Long) {

        for (i in 1..10) {
            val taskInfo =
                "Список задач\n" +
                        "{$i} - Код: |" + "Наименование: |" + "Описание: |" + "Дата начала: |" + "Дата окончания: |" + "Исполнитель: |" + "Статус:"
            sendMessage(chatId, taskInfo)
        }
    }

    private fun startCommandReciver(chatId: Long?, name: String) {
        val answer = EmojiParser.parseToUnicode("Hello ${name}, nice too met you! :blush:")

        logger.info("Replied to user $name")
        sendMessage(chatId, answer, getKeyboard())
    }

    private fun sendMessage(chatId: Long?, textToSend: String, keyboardMarkup: ReplyKeyboardMarkup? = null) {
        val message = SendMessage()
        message.chatId = chatId.toString()
        message.text = textToSend

        keyboardMarkup?.let { message.replyMarkup = keyboardMarkup }

        executeMessage(message)

    }

    private fun executeMessage(message: SendMessage) {
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            logger.error("$ERROR_MSG ${e.message}")
        }
    }

}
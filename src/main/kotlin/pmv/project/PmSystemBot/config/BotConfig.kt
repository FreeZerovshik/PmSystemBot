package pmv.project.PmSystemBot.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("application.properties")
data class BotConfig(
    @Value("\${bot.name}")
    val botName: String,

    @Value("\${bot.token}")
    val token: String
)

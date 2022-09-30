package pmv.project.PmSystemBot.model

import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "users")
class User(

    @Id
    val id: Long = 0,

    val firstName: String? = null,

    val lastName: String? = null,

    val userName: String? = null,

    val registeredAt: Timestamp = Timestamp(System.currentTimeMillis())
)

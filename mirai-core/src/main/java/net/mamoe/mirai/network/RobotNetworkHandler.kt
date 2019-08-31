package net.mamoe.mirai.network

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.mamoe.mirai.MiraiServer
import net.mamoe.mirai.network.packet.client.ClientPacket
import net.mamoe.mirai.network.packet.client.login.*
import net.mamoe.mirai.network.packet.client.session.*
import net.mamoe.mirai.network.packet.client.touch.ClientHeartbeatPacket
import net.mamoe.mirai.network.packet.client.touch.ClientRefreshSKeyRequestPacket
import net.mamoe.mirai.network.packet.client.touch.ServerHeartbeatResponsePacket
import net.mamoe.mirai.network.packet.client.writeHex
import net.mamoe.mirai.network.packet.client.writeRandom
import net.mamoe.mirai.network.packet.server.ServerPacket
import net.mamoe.mirai.network.packet.server.login.*
import net.mamoe.mirai.network.packet.server.security.*
import net.mamoe.mirai.network.packet.server.touch.ServerTouchResponsePacket
import net.mamoe.mirai.network.packet.server.touch.ServerTouchResponsePacketEncrypted
import net.mamoe.mirai.task.MiraiTaskManager
import net.mamoe.mirai.util.*
import net.mamoe.mirai.utils.MiraiLogger
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.system.exitProcess


/**
 * A RobotNetworkHandler is used to connect with Tencent servers.
 *
 * @author Him188moe
 */
@ExperimentalUnsignedTypes
class RobotNetworkHandler(val number: Int, private val password: String) {

    private var sequence: Int = 0

    var socket: DatagramSocket = DatagramSocket((15314 + Math.random() * 100).toInt())

    var serverIP: String = ""
        set(value) {
            serverAddress = InetSocketAddress(value, 8000)
            field = value

            socket.close()
            socket = DatagramSocket((15314 + Math.random() * 100).toInt())
            socket.connect(this.serverAddress)
            val zeroByte: Byte = 0
            Thread {
                while (true) {
                    val dp1 = DatagramPacket(ByteArray(2048), 2048)
                    socket.receive(dp1)
                    GlobalScope.async {
                        var i = dp1.data.size - 1;
                        while (dp1.data[i] == zeroByte) {
                            --i
                        }
                        try {
                            onPacketReceived(ServerPacket.ofByteArray(dp1.data.copyOfRange(0, i + 1)))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }.start()
        }

    private lateinit var serverAddress: InetSocketAddress

    private lateinit var token00BA: ByteArray //这些数据全部是login用的
    private lateinit var token0825: ByteArray
    private var loginTime: Int = 0
    private lateinit var loginIP: String
    private var tgtgtKey: ByteArray? = null
    private var tlv0105: ByteArray
    private lateinit var _0828_rec_decr_key: ByteArray


    private lateinit var sessionKey: ByteArray//这两个是登录成功后得到的
    private lateinit var sKey: String

    /**
     * Used to access web API(for friends list etc.)
     */
    private lateinit var cookies: String
    private var gtk: Int = 0

    init {
        tlv0105 = lazyEncode {
            it.writeHex("01 05 00 30")
            it.writeHex("00 01 01 02 00 14 01 01 00 10")
            it.writeRandom(16)
            it.writeHex("00 14 01 02 00 10")
            it.writeRandom(16)
        }
    }


    @ExperimentalUnsignedTypes
    private var md5_32: ByteArray = getRandomKey(32)


    @ExperimentalUnsignedTypes
    internal fun onPacketReceived(packet: ServerPacket) {
        packet.decode()
        println("Packet received: $packet")
        when (packet) {
            is ServerTouchResponsePacket -> {
                if (packet.serverIP != null) {//redirection
                    serverIP = packet.serverIP!!
                    //connect(packet.serverIP!!)
                    sendPacket(ClientServerRedirectionPacket(packet.serverIP!!, number))
                } else {//password submission
                    this.loginIP = packet.loginIP
                    this.loginTime = packet.loginTime
                    this.token0825 = packet.token0825
                    this.tgtgtKey = packet.tgtgtKey
                    sendPacket(ClientPasswordSubmissionPacket(this.number, this.password, packet.loginTime, packet.loginIP, packet.tgtgtKey, packet.token0825))
                }
            }

            is ServerLoginResponseFailedPacket -> {
                println("Login failed: " + packet.state.toString())
                return
            }

            is ServerLoginResponseVerificationCodePacket -> {
                //[token00BA]来源之一: 验证码
                this.token00BA = packet.token00BA

                with(MiraiServer.getInstance().parentFolder + "verifyCode.png") {
                    ByteArrayInputStream(packet.verifyCode).transferTo(FileOutputStream(this))
                    println("验证码已写入到 " + this.path)
                }

                if (packet.unknownBoolean != null && packet.unknownBoolean!!) {
                    this.sequence = 1
                    sendPacket(ClientLoginVerificationCodePacket(this.number, this.token0825, this.sequence, this.token00BA))
                }

            }

            is ServerLoginResponseSuccessPacket -> {
                this._0828_rec_decr_key = packet._0828_rec_decr_key
                sendPacket(ClientSessionRequestPacket(this.number, this.serverIP, this.loginIP, this.md5_32, packet.token38, packet.token88, packet.encryptionKey, this.tlv0105))
            }

            //是ClientPasswordSubmissionPacket之后服务器回复的
            is ServerLoginResponseResendPacket -> {
                if (packet.tokenUnknown != null) {
                    //this.token00BA = packet.token00BA!!
                    //println("token00BA changed!!! to " + token00BA.toUByteArray())
                }
                if (packet.flag == ServerLoginResponseResendPacket.Flag.`08 36 31 03`) {
                    this.tgtgtKey = packet.tgtgtKey
                    sendPacket(ClientLoginResendPacket3104(
                            this.number,
                            this.password,
                            this.loginTime,
                            this.loginIP,
                            this.tgtgtKey!!,
                            this.token0825,
                            when (packet.tokenUnknown != null) {
                                true -> packet.tokenUnknown!!
                                false -> this.token00BA
                            },
                            packet._0836_tlv0006_encr
                    ))
                } else {
                    sendPacket(ClientLoginResendPacket3106(
                            this.number,
                            this.password,
                            this.loginTime,
                            this.loginIP,
                            this.tgtgtKey!!,
                            this.token0825,
                            when (packet.tokenUnknown != null) {
                                true -> packet.tokenUnknown!!
                                false -> this.token00BA
                            },
                            packet._0836_tlv0006_encr
                    ))
                }
            }

            is ServerSessionKeyResponsePacket -> {
                this.sessionKey = packet.sessionKey
                MiraiTaskManager.getInstance().repeatingTask({
                    sendPacket(ClientHeartbeatPacket(this.number, this.sessionKey))
                }, 90000)
                this.tlv0105 = packet.tlv0105
                sendPacket(ClientLoginStatusPacket(this.number, this.sessionKey, ClientLoginStatus.ONLINE))
            }

            is ServerLoginSuccessPacket -> {
                sendPacket(ClientSKeyRequestPacket(this.number, this.sessionKey))
            }

            is ServerSKeyResponsePacket -> {
                this.sKey = packet.sKey
                this.cookies = "uin=o" + this.number + ";skey=" + this.sKey + ";"

                MiraiTaskManager.getInstance().repeatingTask({
                    sendPacket(ClientRefreshSKeyRequestPacket(this.number, this.sessionKey))
                }, 1800000)
                this.gtk = getGTK(sKey)
                sendPacket(ClientAccountInfoRequestPacket(this.number, this.sessionKey))
            }

            is ServerHeartbeatResponsePacket -> {

            }

            is ServerAccountInfoResponsePacket -> {

            }

            is ServerLoginResponseVerificationCodePacketEncrypted -> onPacketReceived(packet.decrypt())
            is ServerLoginResponseResendPacketEncrypted -> onPacketReceived(packet.decrypt(this.tgtgtKey!!))
            is ServerLoginResponseSuccessPacketEncrypted -> onPacketReceived(packet.decrypt(this.tgtgtKey!!))
            is ServerSessionKeyResponsePacketEncrypted -> onPacketReceived(packet.decrypt(this._0828_rec_decr_key))
            is ServerTouchResponsePacketEncrypted -> onPacketReceived(packet.decrypt())
            is ServerSKeyResponsePacketEncrypted -> onPacketReceived(packet.decrypt(this.sessionKey))
            is ServerAccountInfoResponsePacketEncrypted -> onPacketReceived(packet.decrypt(this.sessionKey))

            else -> throw IllegalArgumentException(packet.toString())
        }

    }

    @ExperimentalUnsignedTypes
    fun sendPacket(packet: ClientPacket) {

        try {
            packet.encode()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        packet.writeHex(Protocol.tail)

        val data = packet.toByteArray()
        try {
            socket.send(DatagramPacket(data, data.size))
            MiraiLogger info "Packet sent: ${data.toUByteArray().toUHexString()}"
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }
}
package com.example.rolegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

// ================= 角色数据 =================
data class RoleConfig(
    val key: String,
    val name: String,
    val shortName: String,
    val hp: Float,
    val speed: Float,
    val size: Float,
    val color: Int
)

val ALL_ROLES: List<RoleConfig> = listOf(
    RoleConfig("death",   "大司命",  "司", 800f,  3.0f, 94f, 0xFFFF4444.toInt()),
    RoleConfig("galo",    "司空震",  "震", 800f,  2.5f, 84f, 0xFFFFAA33.toInt()),
    RoleConfig("glz",     "格力扎",  "格", 500f,  2.5f, 88f, 0xFFAA44FF.toInt()),
    RoleConfig("ayan",    "蒙恬",    "蒙", 1000f, 2.5f, 96f, 0xFFB080FF.toInt()),
    RoleConfig("light",   "光神",    "光", 500f,  3.0f, 84f, 0xFFFFDD44.toInt()),
    RoleConfig("gongben", "宫本",    "宫", 700f,  3.0f, 86f, 0xFFFF6633.toInt()),
    RoleConfig("laobai",  "牢白",    "牢", 600f,  3.0f, 80f, 0xFF44DDFF.toInt()),
    RoleConfig("chenxin", "尘心",    "尘", 550f,  2.8f, 84f, 0xFF66DD88.toInt()),
    RoleConfig("jiaotou", "老教头",  "教", 800f,  3.3f, 88f, 0xFFFF8844.toInt()),
    RoleConfig("lan",     "澜",      "澜", 700f,  5.0f, 82f, 0xFF00CCDD.toInt()),
    RoleConfig("lantest", "澜·测试", "测", 700f,  5.0f, 82f, 0xFFFF66AA.toInt()),
    RoleConfig("anmie",   "黯灭",    "黯", 500f,  3.5f, 86f, 0xFFAA44FF.toInt())
)

// ================= 单位 =================
class Unit(val role: RoleConfig, var team: Int, var x: Float, var y: Float) {
    var hp: Float = role.hp
    val maxHp: Float = role.hp
    var cd: Int = 0
    var dead: Boolean = false
    var isPlayer: Boolean = false

    // 大司命
    var lockedTarget: Unit? = null
    var deathAtkTimer: Int = 0

    // 司空震
    var shield: Float = 0f
    var shieldActive: Boolean = false
    var shieldDuration: Int = 0
    var shieldTriggered: Boolean = false
    var berserkActive: Boolean = false
    var shootCd: Int = 0

    // 格力扎
    var teleportCd: Int = 0
    var blackholeCd: Int = 0

    // 蒙恬
    var boostActive: Boolean = false
    var slashCd: Int = 0

    // 光神
    var lightCd: Int = 0

    // 宫本
    var gbSlashCd: Int = 0
    var gbDashCd: Int = 0
    var antiHealTarget: Unit? = null
    var antiHealTimer: Int = 0
    var blockedHeal: Float = 0f

    // 牢白
    var invincible: Boolean = false
    var invincibleTimer: Int = 0
    var invincibleCd: Int = 0
    var dashTimer: Int = 0
    var isClone: Boolean = false
    var parentUnit: Unit? = null
    var cloneTarget: Unit? = null
    var cloneAttackCd: Int = 0

    // 尘心
    var swordCd: Int = 0

    // 老教头
    var reflectActive: Boolean = false
    var reflectTimer: Int = 0
    var reflectCd: Int = 0
    var reduceActive: Boolean = false
    var reduceTimer: Int = 0
    var reduceCd: Int = 0
    var speedBoostActive: Boolean = false
    var speedBoostTimer: Int = 0
    var lowHpBerserk: Boolean = false
    var trapCd: Int = 0
    var hasTarget: Boolean = false
    var huntedTarget: Unit? = null
    var attackCd: Int = 0

    // 澜
    var trackedTarget: Unit? = null
    var lanDashCd: Int = 0
    var lanIsDashing: Boolean = false
    var lanDashPhase: Int = 0
    var lanDashProgress: Float = 0f
    var lanDashTarget: Unit? = null
    var lanStartX: Float = 0f
    var lanStartY: Float = 0f
    var lanEndX: Float = 0f
    var lanEndY: Float = 0f
    var lanMaxDashes: Int = 2
    var lanRealDamage: Boolean = false
    var lanSpeedBoost: Boolean = false
    var lanHealMult: Float = 0.5f

    // 黯灭
    var isBeaming: Boolean = false
    var beamTimer: Int = 0
    var beamTickTimer: Int = 0
    var beamCd: Int = 0
    var beamStartX: Float = 0f
    var beamStartY: Float = 0f
    var beamEndX: Float = 0f
    var beamEndY: Float = 0f

    val size: Float get() = role.size

    fun distTo(o: Unit): Float {
        val dx = x - o.x
        val dy = y - o.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}

// ================= 子弹 =================
class Bullet(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var dmg: Float, var team: Int,
    var kind: String, var color: Int,
    var radius: Float, var life: Int,
    var owner: Unit?
) {
    var phase: String = "fly"
    var stayTimer: Int = 0
    var targetX: Float = 0f
    var targetY: Float = 0f
    var flySpeed: Float = 4.5f
    var tick: Int = 0
}

// ================= 木桩 =================
class Post(var x: Float, var y: Float, var radius: Float, var life: Int, var target: Unit?)

// ================= 特效 =================
class Effect(
    var x: Float, var y: Float,
    var life: Int, var maxLife: Int,
    var color: Int, var text: String = "",
    var size: Float = 30f
)

// ================= 游戏视图 =================
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val W = 1600f
    private val H = 900f

    private val units = mutableListOf<Unit>()
    private val bullets = mutableListOf<Bullet>()
    private val posts = mutableListOf<Post>()
    private val effects = mutableListOf<Effect>()
    private val toAdd = mutableListOf<Unit>()

    private var gameEnded = false
    private var winner = ""

    private var joystickActive = false
    private var joyCX = 0f
    private var joyCY = 0f
    private var joyX = 0f
    private var joyY = 0f

    private var btnAX = 0f
    private var btnAY = 0f
    private var btnAR = 0f
    private var btn1X = 0f
    private var btn1Y = 0f
    private var btn1R = 0f
    private var btn2X = 0f
    private var btn2Y = 0f
    private var btn2R = 0f

    private var atkCd = 0
    private var s1Cd = 0
    private var s2Cd = 0

    private var playerUnit: Unit? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        running = true
        thread = Thread(this).also { it.start() }
        setup()
    }

    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {
        joyCX = 240f
        joyCY = H - 240f
        btnAX = W - 180f
        btnAY = H - 180f
        btnAR = 110f
        btn1X = W - 400f
        btn1Y = H - 140f
        btn1R = 85f
        btn2X = W - 140f
        btn2Y = H - 400f
        btn2R = 85f
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        thread?.join()
    }

    private fun setup() {
        units.clear()
        bullets.clear()
        posts.clear()
        effects.clear()
        toAdd.clear()
        gameEnded = false
        winner = ""
        atkCd = 0
        s1Cd = 0
        s2Cd = 0

        // 红队（玩家 + 5 AI）
        val my = Unit(ALL_ROLES[0], 1, 250f, H / 2f)
        my.isPlayer = true
        units.add(my)
        playerUnit = my

        units.add(Unit(ALL_ROLES[1], 1, 150f, 120f))
        units.add(Unit(ALL_ROLES[2], 1, 150f, H - 120f))
        units.add(Unit(ALL_ROLES[3], 1, 280f, 250f))
        units.add(Unit(ALL_ROLES[4], 1, 280f, H - 250f))
        units.add(Unit(ALL_ROLES[5], 1, 400f, H / 2f))

        // 蓝队 6 AI
        units.add(Unit(ALL_ROLES[6], 2, W - 150f, 120f))
        units.add(Unit(ALL_ROLES[7], 2, W - 150f, H - 120f))
        units.add(Unit(ALL_ROLES[8], 2, W - 280f, 250f))
        units.add(Unit(ALL_ROLES[9], 2, W - 280f, H - 250f))
        units.add(Unit(ALL_ROLES[10], 2, W - 400f, H / 2f))
        units.add(Unit(ALL_ROLES[11], 2, W - 150f, H / 2f))
    }

    override fun run() {
        var last = System.currentTimeMillis()
        while (running) {
            val now = System.currentTimeMillis()
            val dt = ((now - last) / 16.666f).coerceAtMost(3f)
            last = now
            update(dt)
            val c = holder.lockCanvas() ?: continue
            try {
                synchronized(holder) { drawGame(c) }
            } finally {
                holder.unlockCanvasAndPost(c)
            }
            Thread.sleep(16)
        }
    }

    // ================== 更新 ==================
    private fun update(dt: Float) {
        if (atkCd > 0) atkCd--
        if (s1Cd > 0) s1Cd--
        if (s2Cd > 0) s2Cd--

        if (gameEnded) {
            effects.forEach { it.life-- }
            effects.removeAll { it.life <= 0 }
            return
        }

        for (u in units) {
            if (u.hp <= 0f) {
                u.dead = true
                continue
            }
            if (u.cd > 0) u.cd--
            if (u.teleportCd > 0) u.teleportCd--

            // 玩家摇杆移动
            if (u.isPlayer) {
                val mag = Math.sqrt((joyX * joyX + joyY * joyY).toDouble()).toFloat()
                if (joystickActive && mag > 0.15f) {
                    val nx = joyX / mag
                    val ny = joyY / mag
                    val sp = u.role.speed * 1.3f * dt
                    u.x += nx * sp * 4f
                    u.y += ny * sp * 4f
                }
            }

            updatePassive(u, dt)

            if (!u.isPlayer) {
                aiAct(u, dt)
            }

            u.x = u.x.coerceIn(u.size / 2, W - u.size / 2)
            u.y = u.y.coerceIn(u.size / 2, H - u.size / 2)
        }

        updateBullets(dt)

        // 木桩计时
        var i = posts.size - 1
        while (i >= 0) {
            val p = posts[i]
            p.life--
            if (p.life <= 0 || p.target == null || p.target!!.hp <= 0f) {
                posts.removeAt(i)
            }
            i--
        }

        val a1 = units.count { it.team == 1 && it.hp > 0f }
        val a2 = units.count { it.team == 2 && it.hp > 0f }
        if (a1 == 0 || a2 == 0) {
            gameEnded = true
            winner = if (a1 > 0) "红方胜利！" else "蓝方胜利！"
        }

        if (toAdd.isNotEmpty()) {
            units.addAll(toAdd)
            toAdd.clear()
        }

        effects.forEach { it.life-- }
        effects.removeAll { it.life <= 0 }
    }

    // ================== 被动 ==================
    private fun updatePassive(u: Unit, dt: Float) {
        when (u.role.key) {
            "jiaotou" -> updateJiaotouPassive(u)
            "galo" -> updateGaloPassive(u)
            "laobai" -> updateLaobaiPassive(u)
            "lan", "lantest" -> updateLanPassive(u)
            "anmie" -> updateAnmieBeam(u)
            "gongben" -> {
                val t = u.antiHealTarget
                if (t != null && u.antiHealTimer > 0) {
                    u.antiHealTimer--
                    if (u.antiHealTimer <= 0) {
                        applyBlockedHeal(t)
                        u.antiHealTarget = null
                    }
                }
            }
            "chenxin" -> {
                if (u.swordCd < 360) u.swordCd++
            }
            "galo" -> {}
        }
    }

    private fun updateJiaotouPassive(u: Unit) {
        if (u.reflectCd > 0) u.reflectCd--
        if (!u.lowHpBerserk) {
            u.reduceCd++
            if (u.reduceCd >= 240) {
                u.reduceActive = true
                u.reduceTimer = 0
                u.reduceCd = 0
                u.reflectActive = true
                u.reflectTimer = 0
                u.reflectCd = 240
                effects.add(Effect(u.x, u.y - 70f, 25, 25, 0xFFFF8844.toInt(), "减伤反伤", 36f))
            }
            if (u.reduceActive) {
                u.reduceTimer++
                if (u.reflectActive) {
                    u.reflectTimer++
                    if (u.reflectTimer >= 60) u.reflectActive = false
                }
                if (u.reduceTimer >= 90) {
                    u.reduceActive = false
                    u.reflectActive = false
                }
                if (!u.speedBoostActive) {
                    u.speedBoostActive = true
                    u.speedBoostTimer = 0
                }
                if (u.speedBoostActive) {
                    u.speedBoostTimer++
                    if (u.speedBoostTimer >= 60) u.speedBoostActive = false
                }
            }
        }
        if (u.hp <= 90f && u.hp > 0f && !u.lowHpBerserk) {
            u.lowHpBerserk = true
            u.reduceActive = false
            u.speedBoostActive = false
            u.reflectActive = false
            effects.add(Effect(u.x, u.y - 100f, 35, 35, 0xFFFF2200.toInt(), "残血狂暴", 40f))
        }

        // AI 陷阱
        if (!u.isPlayer) {
            u.trapCd++
            if (u.trapCd >= 600) {
                val e = findNearestEnemy(u)
                if (e != null && u.distTo(e) < 220f) {
                    u.trapCd = 0
                    val px = (e.x + (Math.random() - 0.5).toFloat() * 60f).coerceIn(40f, W - 40f)
                    val py = (e.y + (Math.random() - 0.5).toFloat() * 60f).coerceIn(40f, H - 40f)
                    posts.add(Post(px, py, 65f, 240, e))
                    effects.add(Effect(px, py, 30, 30, 0xFFFF8844.toInt(), "木桩", 45f))
                }
            }
            if (u.attackCd > 0) u.attackCd--
            for (p in posts) {
                val t = p.target
                if (t != null && u.attackCd <= 0 && u.distTo(t) < 60f + u.size / 2) {
                    dealDamage(t, if (u.lowHpBerserk) 30f else 15f, u, false)
                    u.attackCd = if (u.lowHpBerserk) 30 else 60
                }
            }
        }
    }

    private fun updateGaloPassive(u: Unit) {
        if (u.hp <= 100f && u.hp > 0f && !u.shieldTriggered) {
            u.shieldTriggered = true
            u.shieldActive = true
            u.shieldDuration = 0
            u.shield = 0f
            u.berserkActive = true
            effects.add(Effect(u.x, u.y - 70f, 30, 30, 0xFF66DDFF.toInt(), "护盾", 40f))
        }
        if (u.shieldActive) {
            u.shieldDuration++
            if (u.shieldDuration % 30 == 0 && u.shieldDuration <= 150) u.shield += 50f
            if (u.shieldDuration >= 150) u.shieldActive = false
        }
    }

    private fun updateLaobaiPassive(u: Unit) {
        u.invincibleCd++
        if (u.invincibleCd >= 150 && !u.invincible) {
            u.invincible = true
            u.invincibleTimer = 0
            u.invincibleCd = 0
            effects.add(Effect(u.x, u.y - 60f, 20, 20, 0xFF44DDFF.toInt(), "隐身", 36f))
        }
        if (u.invincible) {
            u.invincibleTimer++
            if (u.invincibleTimer >= 30) u.invincible = false
        }
        if (!u.isPlayer) {
            u.dashTimer++
            if (u.dashTimer >= 60) {
                val e = findNearestEnemy(u)
                if (e != null) {
                    val dx = e.x - u.x
                    val dy = e.y - u.y
                    val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (d < 220f && d > 1f) {
                        val len = minOf(80f, d)
                        u.x += dx / d * len
                        u.y += dy / d * len
                        if (d < 90f) dealDamage(e, 30f, u, false)
                    }
                }
                u.dashTimer = 0
            }
        }
    }

    private fun updateLanPassive(u: Unit) {
        val enemies = units.filter { it.team != u.team && it.hp > 0f }
        val low = enemies.any { it.hp / it.maxHp < 0.1f }
        u.lanSpeedBoost = low
        u.lanRealDamage = low
        u.lanMaxDashes = if (low) 3 else 2
        var t: Unit? = null
        var mh = Float.MAX_VALUE
        for (e in enemies) {
            if (e.hp < mh) {
                mh = e.hp
                t = e
            }
        }
        u.trackedTarget = t

        if (!u.isPlayer && t != null && !u.lanIsDashing) {
            u.lanDashCd++
            if (u.lanDashCd >= 45) {
                val d = u.distTo(t)
                if (d <= 80f + u.size / 2) {
                    u.lanIsDashing = true
                    u.lanDashPhase = 0
                    u.lanDashProgress = 0f
                    u.lanDashTarget = t
                    u.lanStartX = u.x
                    u.lanStartY = u.y
                    u.lanEndX = t.x
                    u.lanEndY = t.y
                    u.lanDashCd = 0
                } else {
                    moveToward(u, t)
                }
            } else {
                moveToward(u, t)
            }
        } else if (!u.isPlayer && !u.lanIsDashing) {
            moveRandom(u)
        }

        if (u.lanIsDashing) {
            u.lanDashProgress += 0.15f
            if (u.lanDashProgress >= 1f) {
                u.lanDashProgress = 0f
                u.lanDashPhase++
                val td = u.lanDashTarget
                if (td != null && td.hp > 0f) {
                    val dmg = if (u.lanRealDamage) 50f else 30f
                    dealDamage(td, dmg, u, u.lanRealDamage)
                    healUnit(u, dmg * u.lanHealMult)
                    effects.add(Effect(td.x, td.y, 12, 12, 0xFF00CCDD.toInt(), "", 30f))
                }
                if (u.lanDashPhase >= u.lanMaxDashes || td == null || td.hp <= 0f) {
                    u.lanIsDashing = false
                } else {
                    u.lanStartX = u.x
                    u.lanStartY = u.y
                    u.lanEndX = td.x
                    u.lanEndY = td.y
                }
            } else {
                val tt = u.lanDashProgress
                val sm = tt * tt * (3f - 2f * tt)
                u.x = u.lanStartX + (u.lanEndX - u.lanStartX) * sm
                u.y = u.lanStartY + (u.lanEndY - u.lanStartY) * sm
            }
        }
    }

    private fun updateAnmieBeam(u: Unit) {
        if (u.isBeaming) {
            u.beamTimer++
            u.beamTickTimer++
            if (u.beamTickTimer >= 6) {
                u.beamTickTimer = 0
                for (o in units) {
                    if (o.team != u.team && o.hp > 0f && !o.dead) {
                        val vx = u.beamEndX - u.beamStartX
                        val vy = u.beamEndY - u.beamStartY
                        val wx = o.x - u.beamStartX
                        val wy = o.y - u.beamStartY
                        val c1 = vx * wx + vy * wy
                        val c2 = vx * vx + vy * vy
                        val tt = if (c2 > 0f) (c1 / c2).coerceIn(0f, 1f) else 0f
                        val px = u.beamStartX + vx * tt
                        val py = u.beamStartY + vy * tt
                        val ddx = o.x - px
                        val ddy = o.y - py
                        val d = Math.sqrt((ddx * ddx + ddy * ddy).toDouble()).toFloat()
                        if (d < o.size / 2 + 25f) {
                            dealDamage(o, 20f, u, true)
                        }
                    }
                }
            }
            if (u.beamTimer >= 180) {
                u.isBeaming = false
                u.beamTimer = 0
                u.beamTickTimer = 0
            }
        } else if (!u.isPlayer) {
            u.beamCd++
            if (u.beamCd >= 420) {
                val e = findNearestEnemy(u)
                if (e != null) {
                    startAnmieBeam(u, e)
                } else {
                    u.beamCd = 300
                }
            }
        }
    }

    private fun startAnmieBeam(u: Unit, target: Unit) {
        u.beamStartX = u.x
        u.beamStartY = u.y
        val ang = Math.atan2((target.y - u.y).toDouble(), (target.x - u.x).toDouble()).toFloat()
        val cosA = Math.cos(ang.toDouble()).toFloat()
        val sinA = Math.sin(ang.toDouble()).toFloat()
        var lo = 0f
        var hi = 3000f
        for (i in 0 until 20) {
            val mid = (lo + hi) / 2f
            val mx = u.beamStartX + cosA * mid
            val my = u.beamStartY + sinA * mid
            if (mx >= 0f && mx <= W && my >= 0f && my <= H) lo = mid else hi = mid
        }
        u.beamEndX = u.beamStartX + cosA * lo
        u.beamEndY = u.beamStartY + sinA * lo
        u.isBeaming = true
        u.beamTimer = 0
        u.beamTickTimer = 0
        u.beamCd = 0
        effects.add(Effect(u.x, u.y - 70f, 30, 30, 0xFFAA44FF.toInt(), "光束", 40f))
    }

    // ================== AI ==================
    private fun aiAct(u: Unit, dt: Float) {
        val key = u.role.key
        if (u.lanIsDashing) return
        if (u.isBeaming) return

        val t = findNearestEnemy(u)

        when (key) {
            "death" -> {
                if (u.lockedTarget == null || u.lockedTarget!!.hp <= 0f) u.lockedTarget = t
                val tg = u.lockedTarget
                if (tg != null) {
                    if (u.distTo(tg) > 50f + u.size / 2) {
                        moveToward(u, tg)
                    } else {
                        u.deathAtkTimer++
                        if (u.deathAtkTimer >= 15) {
                            dealDamage(tg, 10f, u, false)
                            healUnit(u, 10f)
                            u.deathAtkTimer = 0
                        }
                    }
                } else moveRandom(u)
            }
            "galo" -> {
                if (t != null) {
                    if (u.distTo(t) > 300f) moveToward(u, t) else moveRandom(u)
                    if (u.shootCd > 0) u.shootCd--
                    if (u.shootCd <= 0) {
                        val dx = t.x - u.x
                        val dy = t.y - u.y
                        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        if (d > 1f) {
                            val spd = if (u.berserkActive) 8f else 7f
                            bullets.add(Bullet(u.x, u.y, dx / d * spd, dy / d * spd,
                                if (u.berserkActive) 30f else 20f, u.team, "arrow",
                                if (u.berserkActive) 0xFFFF6633.toInt() else 0xFFFFAA33.toInt(),
                                if (u.berserkActive) 16f else 12f, 60, u))
                            u.shootCd = if (u.berserkActive) 15 else 30
                        }
                    }
                } else moveRandom(u)
            }
            "glz" -> {
                moveRandom(u)
                if (u.blackholeCd > 0) u.blackholeCd--
                if (u.blackholeCd <= 0) {
                    val ang = Math.random() * Math.PI * 2
                    val dist = 150f + Math.random().toFloat() * 100f
                    val tx = (u.x + Math.cos(ang).toFloat() * dist).coerceIn(40f, W - 40f)
                    val ty = (u.y + Math.sin(ang).toFloat() * dist).coerceIn(40f, H - 40f)
                    val b = Bullet(u.x, u.y, 0f, 0f, 10f, u.team, "blackhole",
                        0xFFAA44FF.toInt(), 50f, 200, u)
                    b.phase = "fly"
                    b.targetX = tx
                    b.targetY = ty
                    b.flySpeed = 5f
                    bullets.add(b)
                    u.blackholeCd = 120
                }
            }
            "ayan" -> {
                if (t != null) {
                    if (u.distTo(t) > 70f + u.size / 2) moveToward(u, t) else moveRandom(u)
                    if (u.slashCd > 0) u.slashCd--
                    if (u.slashCd <= 0 && u.distTo(t) < 80f + u.size / 2) {
                        val dmg = if (u.hp < 100f) 60f else 30f
                        u.boostActive = u.hp < 100f
                        dealDamage(t, dmg, u, false)
                        effects.add(Effect(u.x, u.y, 15, 15, 0xFFB080FF.toInt(), "", 70f))
                        u.slashCd = 30
                    }
                } else moveRandom(u)
            }
            "light" -> {
                moveRandom(u)
                if (u.lightCd > 0) u.lightCd--
                if (u.lightCd <= 0) {
                    for (i in 0 until 8) {
                        val a = (i / 8f) * Math.PI.toFloat() * 2f
                        bullets.add(Bullet(u.x, u.y, Math.cos(a.toDouble()).toFloat() * 6f,
                            Math.sin(a.toDouble()).toFloat() * 6f,
                            40f, u.team, "orb", 0xFFFFDD44.toInt(), 16f, 50, u))
                    }
                    u.lightCd = 150
                }
            }
            "gongben" -> {
                if (t != null) {
                    if (u.distTo(t) > 60f + u.size / 2) moveToward(u, t) else moveRandom(u)
                    if (u.gbSlashCd > 0) u.gbSlashCd--
                    if (u.gbSlashCd <= 0) {
                        var hit = false
                        for (o in units) {
                            if (o.team != u.team && o.hp > 0f && u.distTo(o) < 70f + u.size / 2) {
                                dealDamage(o, 20f, u, false)
                                hit = true
                            }
                        }
                        if (hit) effects.add(Effect(u.x, u.y, 15, 15, 0xFFFF6633.toInt(), "", 70f))
                        u.gbSlashCd = 60
                    }
                    if (u.gbDashCd > 0) u.gbDashCd--
                    if (u.gbDashCd <= 0 && u.distTo(t) < 350f) {
                        val dx = t.x - u.x
                        val dy = t.y - u.y
                        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        if (d > 1f) {
                            u.x = t.x - dx / d * 30f
                            u.y = t.y - dy / d * 30f
                            dealDamage(t, 80f, u, false)
                            u.antiHealTarget = t
                            u.antiHealTimer = 150
                            t.blockedHeal = 0f
                            effects.add(Effect(t.x, t.y, 30, 30, 0xFF8844FF.toInt(), "禁疗", 40f))
                            u.gbDashCd = 360
                        }
                    }
                } else moveRandom(u)
            }
            "laobai" -> {
                if (!u.isPlayer) moveRandom(u)
            }
            "chenxin" -> {
                moveRandom(u)
                if (u.swordCd >= 360 && t != null) {
                    for (i in 0 until 10) {
                        val a = (i / 10f) * Math.PI.toFloat() * 2f
                        val sd = 30f + Math.random().toFloat() * 20f
                        val sx = u.x + Math.cos(a.toDouble()).toFloat() * sd
                        val sy = u.y + Math.sin(a.toDouble()).toFloat() * sd
                        val dx = t.x - sx
                        val dy = t.y - sy
                        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        if (d > 1f) {
                            val spd = 7f
                            bullets.add(Bullet(sx, sy, dx / d * spd, dy / d * spd,
                                20f, u.team, "sword", 0xFF66DD88.toInt(), 10f, 80, u))
                        }
                    }
                    u.swordCd = 0
                    effects.add(Effect(u.x, u.y, 25, 25, 0xFF66DD88.toInt(), "剑阵", 40f))
                }
            }
            "jiaotou" -> {
                val tg = u.huntedTarget
                if (tg != null && tg.hp > 0f) moveToward(u, tg) else {
                    if (t != null) moveToward(u, t) else moveRandom(u)
                }
            }
            "lan", "lantest" -> {
                // 已在 passive 处理
            }
            "anmie" -> {
                if (!u.isBeaming) moveRandom(u)
            }
        }
    }

    private fun findNearestEnemy(u: Unit): Unit? {
        var best: Unit? = null
        var bd = Float.MAX_VALUE
        for (o in units) {
            if (o.team != u.team && o.hp > 0f) {
                val d = u.distTo(o)
                if (d < bd) {
                    bd = d
                    best = o
                }
            }
        }
        return best
    }

    private fun moveToward(u: Unit, t: Unit) {
        val dx = t.x - u.x
        val dy = t.y - u.y
        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (d > u.size * 0.5f) {
            val spd = u.role.speed
            u.x += dx / d * spd
            u.y += dy / d * spd
        }
    }

    private fun moveRandom(u: Unit) {
        if (Math.random() < 0.02) {
            val ang = Math.random() * Math.PI * 2
            u.x += Math.cos(ang).toFloat() * u.role.speed
            u.y += Math.sin(ang).toFloat() * u.role.speed
        }
    }

    // ================== 伤害 ==================
    private fun dealDamage(target: Unit, dmg: Float, source: Unit?, isTrue: Boolean) {
        if (target.hp <= 0f) return

        // 大司命斩杀
        if (source != null && source.role.key == "death" && target.hp <= target.maxHp * 0.08f) {
            target.hp = 0f
            target.dead = true
            effects.add(Effect(target.x, target.y, 30, 30, 0xFFFF0000.toInt(), "斩杀", 50f))
            return
        }

        // 老教头反伤
        if (target.role.key == "jiaotou" && target.reflectActive && source != null && source.hp > 0f && source.team != target.team) {
            dealDamage(source, dmg, target, true)
            effects.add(Effect(source.x, source.y - 30f, 15, 15, 0xFFFFAA00.toInt(), "反伤", 30f))
        }

        var remaining = dmg

        // 司空震护盾
        if (target.role.key == "galo" && target.shield > 0f && !isTrue) {
            val absorb = minOf(target.shield, remaining)
            target.shield -= absorb
            remaining -= absorb
        }

        // 老教头减伤
        if (target.role.key == "jiaotou" && !isTrue) {
            val red: Float = when {
                target.lowHpBerserk -> 0.95f
                target.reduceActive -> 0.75f
                else -> 0f
            }
            if (red > 0f) {
                remaining *= (1f - red)
                effects.add(Effect(target.x, target.y - 30f, 10, 10, 0xFFFF8844.toInt(), "", 25f))
            }
        }

        // 蒙恬减伤
        if (target.role.key == "ayan" && !isTrue) {
            val red = if (target.hp < 100f) 0.6f else 0.3f
            target.boostActive = target.hp < 100f
            remaining *= (1f - red)
            effects.add(Effect(target.x, target.y - 40f, 10, 10, 0xFF66CCFF.toInt(), "", 25f))
        }

        // 牢白隐身
        if (target.role.key == "laobai" && target.invincible && !isTrue) {
            effects.add(Effect(target.x, target.y - 40f, 10, 10, 0xFF44DDFF.toInt(), "闪避", 30f))
            return
        }

        // 格力扎闪避
        if (target.role.key == "glz" && target.hp > 0f && target.teleportCd <= 0 && !isTrue) {
            val dr = if (target.hp < 200f) 0.8 else 0.5
            if (Math.random() < dr) {
                val hs = target.size / 2
                target.x = (Math.random().toFloat() * (W - target.size) + hs)
                target.y = (Math.random().toFloat() * (H - target.size) + hs)
                target.teleportCd = 20
                effects.add(Effect(target.x, target.y, 18, 18, 0xFF88DDFF.toInt(), "闪避", 30f))
                return
            }
        }

        target.hp -= remaining
        if (target.hp < 0f) target.hp = 0f
        effects.add(Effect(target.x, target.y, 12, 12, 0xFFFF6666.toInt(), "", 30f))

        if (target.hp <= 0f && !target.dead) {
            target.dead = true
            effects.add(Effect(target.x, target.y, 30, 30, 0xFFFF4444.toInt(), "击杀", 50f))
        }

        // 牢白受击分身
        if (target.role.key == "laobai" && !target.isClone && target.hp > 0f && source != null && source.team != target.team) {
            spawnClonesForLaobai(target, source)
        }
    }

    private fun spawnClonesForLaobai(owner: Unit, attacker: Unit) {
        if (owner.hp <= 0f || owner.isClone) return
        val cur = units.count { it.isClone && it.parentUnit === owner && it.hp > 0f } + toAdd.count { it.isClone && it.parentUnit === owner }
        if (cur >= 12) return
        val n = minOf(2, 12 - cur)
        for (i in 0 until n) {
            val a = Math.random() * Math.PI * 2
            val dist = 30f + Math.random().toFloat() * 30f
            val cx = (owner.x + Math.cos(a).toFloat() * dist).coerceIn(40f, W - 40f)
            val cy = (owner.y + Math.sin(a).toFloat() * dist).coerceIn(40f, H - 40f)
            val cl = Unit(ALL_ROLES[6], owner.team, cx, cy)
            cl.isClone = true
            cl.parentUnit = owner
            cl.hp = 50f
            cl.cloneTarget = attacker
            toAdd.add(cl)
            effects.add(Effect(cx, cy, 25, 25, 0xFF88DDFF.toInt(), "分身", 30f))
        }
    }

    private fun healUnit(t: Unit, amount: Float) {
        if (t.hp <= 0f) return
        if (t.antiHealTimer > 0) {
            t.blockedHeal += amount
            return
        }
        t.hp = (t.hp + amount).coerceAtMost(t.maxHp)
    }

    private fun applyBlockedHeal(t: Unit) {
        if (t.blockedHeal <= 0f) return
        val amt = t.blockedHeal
        t.blockedHeal = 0f
        val real = minOf(t.maxHp - t.hp, amt)
        if (real > 0f) {
            t.hp += real
            effects.add(Effect(t.x, t.y - 30f, 20, 20, 0xFFFFDD44.toInt(), "回血", 30f))
        }
    }

    // ================== 子弹 ==================
    private fun updateBullets(dt: Float) {
        var i = bullets.size - 1
        while (i >= 0) {
            val b = bullets[i]
            if (b.kind == "blackhole") {
                if (b.phase == "fly") {
                    val dx = b.targetX - b.x
                    val dy = b.targetY - b.y
                    val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (d < b.flySpeed) {
                        b.x = b.targetX
                        b.y = b.targetY
                        b.phase = "stay"
                        b.stayTimer = 0
                    } else {
                        b.x += dx / d * b.flySpeed
                        b.y += dy / d * b.flySpeed
                    }
                } else {
                    b.stayTimer++
                    b.tick++
                    if (b.tick % 6 == 0) {
                        for (o in units) {
                            if (o.team != b.team && o.hp > 0f && o.distToPoint(b.x, b.y) < b.radius) {
                                dealDamage(o, 10f, b.owner, false)
                            }
                        }
                    }
                    if (b.stayTimer >= 150) {
                        bullets.removeAt(i)
                        i--
                        continue
                    }
                }
                i--
                continue
            }

            b.x += b.vx
            b.y += b.vy
            b.life--
            if (b.x < 0f || b.x > W || b.y < 0f || b.y > H || b.life <= 0) {
                bullets.removeAt(i)
                i--
                continue
            }
            var hit = false
            for (o in units) {
                if (o.team != b.team && o.hp > 0f) {
                    val hitR = b.radius + o.size * 0.35f
                    if (o.distToPoint(b.x, b.y) < hitR) {
                        dealDamage(o, b.dmg, b.owner, false)
                        hit = true
                        break
                    }
                }
            }
            if (hit) {
                bullets.removeAt(i)
                i--
                continue
            }
            i--
        }
    }

    // 给 Unit 加个工具方法
    private fun Unit.distToPoint(px: Float, py: Float): Float {
        val dx = x - px
        val dy = y - py
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    // ================== 玩家攻击 / 技能 ==================
    private fun triggerPlayerAttack() {
        if (atkCd > 0) return
        val u = playerUnit ?: return
        if (u.hp <= 0f) return
        val t = findNearestEnemy(u) ?: return
        val d = u.distTo(t)
        if (d < 120f) {
            dealDamage(t, 15f, u, false)
            atkCd = 20
        } else {
            // 距离太远，直接发射一发弹
            val dx = t.x - u.x
            val dy = t.y - u.y
            val dd = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dd > 1f) {
                bullets.add(Bullet(u.x, u.y, dx / dd * 12f, dy / dd * 12f,
                    15f, u.team, "arrow", u.role.color, 14f, 60, u))
                atkCd = 20
            }
        }
    }

    private fun triggerPlayerSkill1() {
        if (s1Cd > 0) return
        val u = playerUnit ?: return
        if (u.hp <= 0f) return
        val t = findNearestEnemy(u) ?: return

        when (u.role.key) {
            "death" -> {
                dealDamage(t, 30f, u, false)
                healUnit(u, 20f)
                effects.add(Effect(t.x, t.y, 20, 20, 0xFFFF4444.toInt(), "死亡凝视", 40f))
                s1Cd = 60
            }
            "galo" -> {
                val dx = t.x - u.x
                val dy = t.y - u.y
                val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (d > 1f) {
                    u.x += dx / d * 150f
                    u.y += dy / d * 150f
                    dealDamage(t, 40f, u, false)
                }
                effects.add(Effect(u.x, u.y, 20, 20, 0xFFFFAA33.toInt(), "雷霆冲刺", 40f))
                s1Cd = 60
            }
            "glz" -> {
                val ang = Math.random() * Math.PI * 2
                val tx = (u.x + Math.cos(ang).toFloat() * 250f).coerceIn(40f, W - 40f)
                val ty = (u.y + Math.sin(ang).toFloat() * 250f).coerceIn(40f, H - 40f)
                val b = Bullet(u.x, u.y, 0f, 0f, 15f, u.team, "blackhole",
                    0xFFAA44FF.toInt(), 50f, 200, u)
                b.targetX = tx
                b.targetY = ty
                b.flySpeed = 5f
                bullets.add(b)
                s1Cd = 60
            }
            "ayan" -> {
                for (o in units) {
                    if (o.team != u.team && o.hp > 0f && u.distTo(o) < 100f) {
                        dealDamage(o, 40f, u, false)
                    }
                }
                effects.add(Effect(u.x, u.y, 20, 20, 0xFFB080FF.toInt(), "铁骑冲击", 90f))
                s1Cd = 45
            }
            "light" -> {
                for (i in 0 until 12) {
                    val a = (i / 12f) * Math.PI.toFloat() * 2f
                    bullets.add(Bullet(u.x, u.y,
                        Math.cos(a.toDouble()).toFloat() * 7f,
                        Math.sin(a.toDouble()).toFloat() * 7f,
                        60f, u.team, "orb", 0xFFFFDD44.toInt(), 16f, 60, u))
                }
                s1Cd = 90
            }
            "gongben" -> {
                val d = u.distTo(t)
                if (d < 400f) {
                    val dx = t.x - u.x
                    val dy = t.y - u.y
                    u.x = t.x - dx / d * 30f
                    u.y = t.y - dy / d * 30f
                    dealDamage(t, 80f, u, false)
                    u.antiHealTarget = t
                    u.antiHealTimer = 150
                    t.blockedHeal = 0f
                    effects.add(Effect(t.x, t.y, 25, 25, 0xFF8844FF.toInt(), "禁疗", 40f))
                }
                s1Cd = 60
            }
            "laobai" -> {
                val dx = t.x - u.x
                val dy = t.y - u.y
                val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (d > 1f) {
                    val len = minOf(150f, d)
                    u.x += dx / d * len
                    u.y += dy / d * len
                    if (d < 100f) dealDamage(t, 30f, u, false)
                }
                effects.add(Effect(u.x, u.y, 20, 20, 0xFF44DDFF.toInt(), "隐者突袭", 40f))
                s1Cd = 30
            }
            "chenxin" -> {
                for (i in 0 until 8) {
                    val a = (i / 8f) * Math.PI.toFloat() * 2f
                    val sd = 30f
                    val sx = u.x + Math.cos(a.toDouble()).toFloat() * sd
                    val sy = u.y + Math.sin(a.toDouble()).toFloat() * sd
                    val dx = t.x - sx
                    val dy = t.y - sy
                    val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (d > 1f) {
                        bullets.add(Bullet(sx, sy, dx / d * 8f, dy / d * 8f,
                            25f, u.team, "sword", 0xFF66DD88.toInt(), 10f, 80, u))
                    }
                }
                s1Cd = 30
            }
            "jiaotou" -> {
                val px = (t.x + (Math.random() - 0.5).toFloat() * 60f).coerceIn(40f, W - 40f)
                val py = (t.y + (Math.random() - 0.5).toFloat() * 60f).coerceIn(40f, H - 40f)
                posts.add(Post(px, py, 65f, 180, t))
                effects.add(Effect(px, py, 30, 30, 0xFFFF8844.toInt(), "木桩", 50f))
                s1Cd = 120
            }
            "lan" -> {
                val dmg = if (u.lanRealDamage) 40f else 25f
                dealDamage(t, dmg, u, u.lanRealDamage)
                healUnit(u, dmg * u.lanHealMult)
                val dx = t.x - u.x
                val dy = t.y - u.y
                val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (d > 1f) {
                    u.x = t.x + dx / d * 30f
                    u.y = t.y + dy / d * 30f
                }
                effects.add(Effect(t.x, t.y, 15, 15, 0xFF00CCDD.toInt(), "", 30f))
                s1Cd = 45
            }
            "lantest" -> {
                val dmg = if (u.lanRealDamage) 40f else 25f
                dealDamage(t, dmg, u, u.lanRealDamage)
                healUnit(u, dmg * 1.0f)
                effects.add(Effect(t.x, t.y, 15, 15, 0xFFFF66AA.toInt(), "", 30f))
                s1Cd = 45
            }
            "anmie" -> {
                if (!u.isBeaming) {
                    startAnmieBeam(u, t)
                    s1Cd = 60
                }
            }
        }
    }

    private fun triggerPlayerSkill2() {
        if (s2Cd > 0) return
        val u = playerUnit ?: return
        if (u.hp <= 0f) return
        val t = findNearestEnemy(u) ?: return
        // 通用范围爆发
        for (o in units) {
            if (o.team != u.team && o.hp > 0f && u.distTo(o) < 200f) {
                dealDamage(o, 25f, u, false)
            }
        }
        effects.add(Effect(u.x, u.y, 20, 20, 0xFFFF8844.toInt(), "范围爆发", 90f))
        s2Cd = 60
    }

    // ================== 绘制 ==================
    private fun drawGame(c: Canvas) {
        c.drawColor(0xFF0B1220.toInt())

        // 网格
        paint.color = 0xFF1F334A.toInt()
        paint.strokeWidth = 1f
        var gx = 0f
        while (gx < W) {
            c.drawLine(gx, 0f, gx, H, paint)
            gx += 60f
        }
        var gy = 0f
        while (gy < H) {
            c.drawLine(0f, gy, W, gy, paint)
            gy += 60f
        }

        // 木桩
        for (p in posts) {
            paint.color = 0xFF8B7355.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            c.drawCircle(p.x, p.y, p.radius, paint)
            paint.style = Paint.Style.FILL
            paint.color = 0xFFD4A574.toInt()
            c.drawCircle(p.x, p.y, 10f, paint)
        }

        // 光束
        for (u in units) {
            if (u.role.key == "anmie" && u.isBeaming && u.hp > 0f) {
                paint.color = 0xAAFF44FF.toInt()
                paint.strokeWidth = u.size * 0.7f
                paint.style = Paint.Style.STROKE
                c.drawLine(u.beamStartX, u.beamStartY, u.beamEndX, u.beamEndY, paint)
                paint.color = 0x66FFCCFF.toInt()
                paint.strokeWidth = u.size * 0.25f
                c.drawLine(u.beamStartX, u.beamStartY, u.beamEndX, u.beamEndY, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // 单位
        for (u in units) {
            paint.alpha = if (u.hp <= 0f || u.dead) 90 else 255

            val half = u.size / 2f

            // 队伍光晕
            paint.color = if (u.team == 1) 0x44FF4444 else 0x444488FF
            c.drawCircle(u.x, u.y, half * 2f, paint)

            // 主体
            paint.color = u.role.color
            c.drawCircle(u.x, u.y, half, paint)

            // 内圆
            paint.color = 0x55000000
            c.drawCircle(u.x, u.y, half * 0.85f, paint)

            // 短名
            textPaint.color = 0xFFFFFFFF.toInt()
            textPaint.textSize = half * 0.9f
            c.drawText(u.role.shortName, u.x, u.y + half * 0.35f, textPaint)

            // 玩家标识
            if (u.isPlayer) {
                paint.style = Paint.Style.STROKE
                paint.color = 0xFFFFDD44.toInt()
                paint.strokeWidth = 5f
                c.drawCircle(u.x, u.y, half + 20f, paint)
                paint.style = Paint.Style.FILL
                textPaint.color = 0xFFFFDD44.toInt()
                textPaint.textSize = 24f
                c.drawText("你", u.x, u.y - half - 30f, textPaint)
            }

            // 血条
            val barW = u.size + 40f
            val barY = u.y - half - 30f
            paint.color = 0xDD000000.toInt()
            c.drawRect(u.x - barW / 2f, barY, u.x + barW / 2f, barY + 14f, paint)
            val hpPct = u.hp / u.maxHp
            paint.color = if (hpPct > 0.5f) 0xFF4CD964.toInt() else 0xFFFF6B6B.toInt()
            c.drawRect(u.x - barW / 2f, barY, u.x - barW / 2f + barW * hpPct, barY + 14f, paint)

            // 名字 + 血量
            textPaint.color = 0xFFFFFFFF.toInt()
            textPaint.textSize = 20f
            c.drawText(u.role.name + " " + u.hp.toInt() + "/" + u.maxHp.toInt(), u.x, barY - 8f, textPaint)

            // 状态
            var tagY = barY - 34f
            if (u.role.key == "jiaotou") {
                if (u.lowHpBerserk) { textPaint.color = 0xFFFF2200.toInt(); c.drawText("狂暴", u.x, tagY, textPaint); tagY -= 22f }
                else if (u.reflectActive) { textPaint.color = 0xFFFFAA00.toInt(); c.drawText("反伤", u.x, tagY, textPaint); tagY -= 22f }
                else if (u.reduceActive) { textPaint.color = 0xFFFF8844.toInt(); c.drawText("减伤75%", u.x, tagY, textPaint); tagY -= 22f }
            }
            if (u.role.key == "galo" && u.shield > 0f) {
                textPaint.color = 0xFF66DDFF.toInt()
                c.drawText("护盾" + u.shield.toInt(), u.x, tagY, textPaint)
                tagY -= 22f
            }
            if (u.role.key == "laobai" && u.invincible) {
                textPaint.color = 0xFF44DDFF.toInt()
                c.drawText("隐身", u.x, tagY, textPaint)
                tagY -= 22f
            }
            if (u.role.key == "death") {
                textPaint.color = 0xFFFF6666.toInt()
                c.drawText("8%斩杀", u.x, tagY, textPaint)
                tagY -= 22f
            }
            if ((u.role.key == "lan" || u.role.key == "lantest") && u.lanRealDamage) {
                textPaint.color = 0xFF00DDFF.toInt()
                c.drawText("斩杀", u.x, tagY, textPaint)
                tagY -= 22f
            }

            // 特效光环
            if (u.invincible || u.reflectActive) {
                paint.style = Paint.Style.STROKE
                paint.color = if (u.invincible) 0xFF44DDFF.toInt() else 0xFFFFAA00.toInt()
                paint.strokeWidth = 6f
                c.drawCircle(u.x, u.y, half + 25f, paint)
                paint.style = Paint.Style.FILL
            }
            if (u.isClone) {
                paint.style = Paint.Style.STROKE
                paint.color = 0xFF88DDFF.toInt()
                paint.strokeWidth = 3f
                c.drawCircle(u.x, u.y, half + 8f, paint)
                paint.style = Paint.Style.FILL
            }

            paint.alpha = 255
        }

        // 特效
        for (e in effects) {
            val alpha = (e.life * 255 / e.maxLife).coerceIn(0, 255)
            paint.alpha = alpha
            paint.color = e.color
            c.drawCircle(e.x, e.y, e.size, paint)
            if (e.text.isNotEmpty()) {
                textPaint.alpha = alpha
                textPaint.color = e.color
                textPaint.textSize = 24f
                c.drawText(e.text, e.x, e.y - e.size - 10f, textPaint)
                textPaint.alpha = 255
            }
            paint.alpha = 255
        }

        // 子弹
        for (b in bullets) {
            paint.color = b.color
            c.drawCircle(b.x, b.y, b.radius, paint)
        }

        // 摇杆
        if (joystickActive) {
            paint.color = 0x33FFFFFF
            c.drawCircle(joyCX, joyCY, 180f, paint)
            paint.color = 0x88FFFFFF.toInt()
            c.drawCircle(joyCX + joyX * 100f, joyCY + joyY * 100f, 70f, paint)
        } else {
            paint.color = 0x22FFFFFF
            c.drawCircle(joyCX, joyCY, 180f, paint)
            paint.color = 0x55FFFFFF
            c.drawCircle(joyCX, joyCY, 70f, paint)
        }

        // 按钮
        drawBtn(c, btn1X, btn1Y, btn1R, "1", "技1", 0xAA4A7ACC.toInt(), s1Cd)
        drawBtn(c, btn2X, btn2Y, btn2R, "2", "技2", 0xAA7A4ACC.toInt(), s2Cd)
        drawBtn(c, btnAX, btnAY, btnAR, "A", "普攻", 0xCCCC3333.toInt(), atkCd)

        // 结束界面
        if (gameEnded) {
            paint.color = 0xCC000000.toInt()
            c.drawRect(0f, H / 2 - 200f, W, H / 2 + 200f, paint)
            textPaint.color = 0xFFFFF4C2.toInt()
            textPaint.textSize = 100f
            c.drawText(winner, W / 2, H / 2 + 20f, textPaint)
            textPaint.color = 0xFF8899BB.toInt()
            textPaint.textSize = 36f
            c.drawText("点击屏幕重新开始", W / 2, H / 2 + 100f, textPaint)
        }
    }

    private fun drawBtn(c: Canvas, x: Float, y: Float, r: Float, icon: String, label: String, color: Int, cd: Int) {
        paint.color = color
        c.drawCircle(x, y, r, paint)
        paint.style = Paint.Style.STROKE
        paint.color = 0x88FFFFFF.toInt()
        paint.strokeWidth = 4f
        c.drawCircle(x, y, r, paint)
        paint.style = Paint.Style.FILL
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = r * 0.85f
        c.drawText(icon, x, y + r * 0.15f, textPaint)
        textPaint.textSize = r * 0.32f
        c.drawText(label, x, y + r * 0.72f, textPaint)
        if (cd > 0) {
            paint.color = 0xAA000000.toInt()
            c.drawCircle(x, y, r, paint)
            textPaint.color = 0xFFFF8844.toInt()
            textPaint.textSize = r * 0.7f
            c.drawText(cd.toString(), x, y + r * 0.25f, textPaint)
        }
    }

    // ================== 触摸 ==================
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val px = event.getX(idx)
                val py = event.getY(idx)

                // 摇杆
                val dxj = px - joyCX
                val dyj = py - joyCY
                if (Math.sqrt((dxj * dxj + dyj * dyj).toDouble()) < 220.0) {
                    joystickActive = true
                    joyX = dxj / 180f
                    joyY = dyj / 180f
                    return true
                }

                // 按钮
                val dA = Math.sqrt(((px - btnAX) * (px - btnAX) + (py - btnAY) * (py - btnAY)).toDouble())
                if (dA < btnAR + 30.0) { triggerPlayerAttack(); return true }

                val d1 = Math.sqrt(((px - btn1X) * (px - btn1X) + (py - btn1Y) * (py - btn1Y)).toDouble())
                if (d1 < btn1R + 30.0) { triggerPlayerSkill1(); return true }

                val d2 = Math.sqrt(((px - btn2X) * (px - btn2X) + (py - btn2Y) * (py - btn2Y)).toDouble())
                if (d2 < btn2R + 30.0) { triggerPlayerSkill2(); return true }

                if (gameEnded) { setup(); return true }
            }
            MotionEvent.ACTION_MOVE -> {
                if (joystickActive) {
                    joyX = (event.x - joyCX) / 180f
                    joyY = (event.y - joyCY) / 180f
                    val m = Math.sqrt((joyX * joyX + joyY * joyY).toDouble()).toFloat()
                    if (m > 1f) {
                        joyX /= m
                        joyY /= m
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                joystickActive = false
                joyX = 0f
                joyY = 0f
            }
        }
        return true
    }
}

// ================= Activity =================
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(GameView(this))
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }
}

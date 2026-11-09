package com.lckdn.app.vpn

/** Minimal IPv4 / UDP / DNS helpers for the on-device DNS filter. */
object Packets {

    fun u16(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    private fun put16(b: ByteArray, off: Int, v: Int) {
        b[off] = ((v ushr 8) and 0xFF).toByte()
        b[off + 1] = (v and 0xFF).toByte()
    }

    /** Lower-cased QNAME of the first question, or null if malformed. */
    fun dnsQuestionName(msg: ByteArray): String? {
        if (msg.size < 12 || u16(msg, 4) < 1) return null
        var i = 12
        val sb = StringBuilder()
        while (i < msg.size) {
            val len = msg[i].toInt() and 0xFF
            if (len == 0) return sb.toString().lowercase()
            if ((len and 0xC0) != 0) return null
            if (i + 1 + len > msg.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (k in 1..len) sb.append((msg[i + k].toInt() and 0xFF).toChar())
            i += 1 + len
        }
        return null
    }

    private fun dnsQuestionEnd(msg: ByteArray): Int? {
        var i = 12
        while (i < msg.size) {
            val len = msg[i].toInt() and 0xFF
            if (len == 0) {
                val end = i + 1 + 4
                return if (end <= msg.size) end else null
            }
            if ((len and 0xC0) != 0) return null
            i += 1 + len
        }
        return null
    }

    /** An NXDOMAIN answer echoing the question. */
    fun dnsBlockedResponse(query: ByteArray): ByteArray? {
        val end = dnsQuestionEnd(query) ?: return null
        val r = query.copyOf(end)
        r[2] = (0x80 or (query[2].toInt() and 0x01)).toByte() // QR=1, RD copied
        r[3] = 0x83.toByte()                                   // RA=1, RCODE=NXDOMAIN
        put16(r, 4, 1)
        put16(r, 6, 0)
        put16(r, 8, 0)
        put16(r, 10, 0)
        return r
    }

    /** Builds the IPv4/UDP reply to [request] (whose IP header length is [ihl]) carrying [payload]. */
    fun udpReply(request: ByteArray, ihl: Int, payload: ByteArray): ByteArray {
        val total = 20 + 8 + payload.size
        val p = ByteArray(total)
        p[0] = 0x45
        put16(p, 2, total)
        put16(p, 6, 0x4000) // don't fragment
        p[8] = 64
        p[9] = 17
        System.arraycopy(request, 16, p, 12, 4) // src = original dst
        System.arraycopy(request, 12, p, 16, 4) // dst = original src
        put16(p, 10, ipChecksum(p, 0, 20))
        put16(p, 20, u16(request, ihl + 2))
        put16(p, 22, u16(request, ihl))
        put16(p, 24, 8 + payload.size)
        // UDP checksum 0 = not computed (valid for IPv4)
        System.arraycopy(payload, 0, p, 28, payload.size)
        return p
    }

    private fun ipChecksum(b: ByteArray, off: Int, len: Int): Int {
        var sum = 0L
        var i = off
        while (i < off + len) {
            sum += u16(b, i)
            i += 2
        }
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }
}

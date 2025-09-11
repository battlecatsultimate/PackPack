@file:Suppress("SameParameterValue")

import mandarin.card.CardBot
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.YDKEValidator


@Suppress("unused")
class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CardBot.test = true
            CardBot.readCardData()

            val inventory = Inventory.getInventory(195682910269865984L)

            val link = "ydke://YWyODFJtjgw/a44MP2uODCFtjgwEbY4MC22ODAttjgwnbY4MJ22ODLFsjgz/a44M/2uODLJrjgyObI4Mam2ODOtqjgwLa44MC2uODJZsjgyWbI4M7GqODOxqjgwJa44MCmuODH1tjgx9bY4M1muODAJsjgwCbI4MQ77dAEmTlgLjsCoD7yX+BPipjAX7mjEDGUOdBfK/hQHblWsC+wR4Ag==!!o2uODCJtjgw9bI4M72uODE5tjgzwa44MoWuODKJrjgxXbI4M7muODLZsjgwFbI4MfG2ODHRHKwFJk5YC!"

            YDKEValidator.loadWhiteListData()
            YDKEValidator.loadCDBData()

            val pair = YDKEValidator.sanitize(inventory, link, YDKEValidator.WhiteList.NORMAL)

            println("Offered Link : $link")
            println(YDKEValidator.toData(link))

            println("Sanitized Link : ${pair.first}")
            println(YDKEValidator.toData(pair.first))

            println("----------")

            pair.second.forEach { println(it) }
        }

        fun upload() {

        }
    }
}
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

            val link = "ydke://YWyODH5sjgyBbI4MZ2yODG9sjgzea44MGmyODBpsjgynbI4Mp2yODKdsjgyua44MrmuODFJtjgxhbY4MAW2ODAFtjgzia44M92qODPdqjgz3ao4MUWuODFFrjgxRa44Mr2uODK9rjgxabY4MWm2ODFptjgyja44Mo2uODDRtjgw0bY4MNG2ODJhtjgyYbY4MmG2ODPZqjgz2ao4M9mqODAxtjgwMbY4MDG2ODJlsjgyZbI4MmWyODL5rjgy+a44MLGyODCxsjgwsbI4M9WqODPVqjgz1ao4MIm2ODCJtjgwibY4MHWuODB1rjgwda44M0GuODNBrjgzQa44MOm2ODD9rjgw/a44MP2uODCFtjgwxbY4M22qODNtqjgzbao4MBG2ODARtjgwEbY4MrmyODEptjgxKbY4MBmyODAttjgwLbY4MC22ODCdtjgwnbY4MJ22ODMNsjgzDbI4Mw2yODLFsjgyxbI4MZm2ODGZtjgy/bI4MPWyODD1sjgw9bI4MGm2ODBptjgwabY4M72uODIdsjgyHbI4Mh2yODE5tjgy7a44Mu2uODGVtjgxlbY4MHGuODBxrjgwca44M8GuODIhsjgyIbI4MiGyODE9sjgxPbI4MT2yODMpqjgzKao4MymqODKJrjgxXbI4M7muODMdsjgzya44M8muODPNqjgzzao4M82qODP9rjgz/a44M/2uODCprjgwqa44MtmyODMxqjgzMao4MzGqODIJrjgw=!!!"

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
@file:Suppress("SameParameterValue")

import mandarin.card.supporter.YDKEValidator


@Suppress("unused")
class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val link = "ydke://gWyODBpsjgyEbI4MrmuODGFtjgwBbY4MMW2ODEptjgxKbY4MGm2ODBptjgy/bI4MT2yODIJrjgyla44M7WuODDxtjgxia44MJG2ODPVrjgz1a44MdGuODGNrjgxja44MZGuODGRrjgxka44M1muODLVsjgy1bI4MWWyODO+CFgCmm/QBxanaBO8l/gS4a60EGUOdBfK/hQHblWsC+wR4Ag==!QWuODI1sjgzAbI4M!o2uODCJtjgw9bI4M72uODFdsjgzwa44MoWuODKJrjgzua44MtmyODAVsjgx8bY4MQ77dAHRHKwHjsCoD!"

            println("Link : $link")

            val data = YDKEValidator.toData(link)

            println("Decoded : $data")

            val back = YDKEValidator.toLink(data)

            println("Encoded : $back")
        }

        fun upload() {

        }
    }
}
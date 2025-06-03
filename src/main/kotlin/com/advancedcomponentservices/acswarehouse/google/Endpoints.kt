package com.advancedcomponentservices.acswarehouse.google

class Endpoints {
    companion object {
        const val profile = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
        const val token = "https://oauth2.googleapis.com/token"
        const val send = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
        const val messages = "https://gmail.googleapis.com/gmail/v1/users/me/messages/"
        const val history = "https://www.googleapis.com/gmail/v1/users/me/history"
        const val oauth = "https://accounts.google.com/o/oauth2/v2/auth"
        const val threads = "https://gmail.googleapis.com/gmail/v1/users/me/threads"
        const val drive = "https://www.googleapis.com/drive/v3/files"
        const val sheets = "https://sheets.googleapis.com"
        const val sheetsRead = "$sheets/v4/spreadsheets/"
        const val batchSheetsRead = "$sheets/v4/spreadsheets/{spreadsheetId}/values:batchGet"
        const val sheetsUpdate = "$sheets/v4/spreadsheets/{spreadsheetId}/values/{range}?valueInputOption={valueInputOption}"
        const val batchSheetsUpdate = "$sheets/v4/spreadsheets/{spreadsheetId}:batchUpdate"
        const val batchSheetsUpdateValues = "$sheets/v4/spreadsheets/{spreadsheetId}/values:batchUpdate"
        const val appendToSheet = "$sheets/v4/spreadsheets/{spreadsheetId}/values/{range}:append"
        const val clearFromSheet = "$sheets/v4/spreadsheets/{spreadsheetId}/values/{range}:clear"
    }
}
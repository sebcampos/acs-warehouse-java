package com.advancedcomponentservices.acswarehouse.google

class Endpoints {
    val profile = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
    val token = "https://oauth2.googleapis.com/token"
    val send = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
    val messages = "https://gmail.googleapis.com/gmail/v1/users/me/messages/"
    val history = "https://www.googleapis.com/gmail/v1/users/me/history"
    val oauth = "https://accounts.google.com/o/oauth2/v2/auth"
    val threads = "https://gmail.googleapis.com/gmail/v1/users/me/threads"

    val drive = "https://www.googleapis.com/drive/v3/files"
    val sheets = "https://sheets.googleapis.com"
    val sheetsRead = "$sheets/v4/spreadsheets/"
    val batchSheetsRead = "$sheets/v4/spreadsheets/{spreadsheetId}/values:batchGet"
    val sheetsUpdate = "$sheets/v4/spreadsheets/{spreadsheetId}/values/{range}?valueInputOption={valueInputOption}"
    val batchSheetsUpdate = "$sheets/v4/spreadsheets/{spreadsheetId}:batchUpdate"
    val batchSheetsUpdateValues = "$sheets/v4/spreadsheets/{spreadsheetId}/values:batchUpdate"
    val appendToSheet = "$sheets/v4/spreadsheets/{spreadsheetId}/values/{range}:append"
    val clearFromSheet = "$sheets/v4/spreadsheets/{spreadsheetId}/values/{range}:clear"
}
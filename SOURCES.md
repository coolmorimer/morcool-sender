# Документация интеграций

Проверено при подготовке предварительной версии 1.1, 6 ноября 2026 года. Это ссылки на документацию поставщиков; интерфейсы и условия платформ могут изменяться.

## Telegram

- [Telegram Bot API — sendMessage](https://core.telegram.org/bots/api#sendmessage): адрес чата, текст и структура результата.
- [Руководство по созданию бота](https://core.telegram.org/bots/tutorial): работа токена и начало общения с ботом.
- [Ссылки Telegram](https://core.telegram.org/api/links): ссылки по username и телефону.
- [Получение API ID для собственного Telegram-клиента](https://core.telegram.org/api/obtaining_api_id): отдельный путь интеграции через клиентский API. В текущем личном режиме используется веб-клиент, а не TDLib.
- [Официальный Telegram Web](https://web.telegram.org/k/): страница личного веб-входа.

## MAX

- [POST /messages](https://dev.max.ru/docs-api/methods/POST/messages): отправка ботом, `user_id`/`chat_id`, заголовок `Authorization` и домен `platform-api2.max.ru`.
- [Документация MAX для партнёров](https://dev.max.ru/docs): создание ботов и доступ к платформе.
- [Open Client API](https://legal.max.ru/openclient-docs): отдельная программа для личных клиентских приложений; требуется одобрение и App-ID. Текущий личный веб-режим не использует этот API.
- [Официальная веб-версия MAX](https://web.max.ru/): страница личного веб-входа.

## WhatsApp и Android

- [WhatsApp Web](https://web.whatsapp.com/): страница личного веб-входа.
- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService): служба экранного управления WhatsApp.
- [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types#data-sync): ограничения фоновой службы синхронизации данных.

## Сертификат API MAX

Документация MAX требует доверенного сертификата для нового API-домена. Публичный корневой сертификат получен с [официального адреса загрузки](https://gu-st.ru/content/lending/russian_trusted_root_ca_pem.crt), связанного с [инструкциями Госуслуг](https://www.gosuslugi.ru/crt).

- Subject/Issuer: `Russian Trusted Root CA`, The Ministry of Digital Development and Communications, RU.
- Срок: 2 марта 2022 — 28 февраля 2032.
- SHA-256 сертификата: `D26D2D0231B7C39F92CC738512BA54103519E4405D68B5BD703E9788CA8ECF31`.
- Файл ресурса: `res/raw/max_root.crt`. Это публичный сертификат, не закрытый ключ.

Дополнительное доверие применяется к соединениям MAX. Проверка цепочки и имени сервера сохраняется; приложение не устанавливает сертификаты в системное хранилище телефона.

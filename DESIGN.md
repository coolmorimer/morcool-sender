# Morcool Sender interface

Native Android Operate interface, Russian copy, Android 6+ API guards. Preserve the established quiet green identity while making the task feel clear and welcoming.

## Visual system

Warm pale green canvas, white grouped form surfaces, dark ink, deep green actions; night theme has a near-black green canvas, tonal green panels, light mint actions and readable secondary text. Rounded 16dp section surfaces, 12–14dp inputs/actions, no decorative gradients or hard shadows. Android sans typography, all text in sp and all touch targets at least 48dp. Native controls receive theme-aware tints. Bottom navigation uses one consistent line-icon system.

## Main flow

The compose screen separates **Кто отправляет**, **Кому отправить**, and **Ваше сообщение** into distinct task blocks. Sender readiness and valid recipient count use current data. **Продвинутые настройки** is collapsed initially and contains the queue title, interval and template saving. The pinned **Проверить и продолжить** action creates a preview only; it does not send. If no valid addresses exist, the preview action is disabled with an explanation.

## Other screens

Contacts put phone import, file import and downloadable CSV in one entry block; the searchable contact list is a second block. Telegram/MAX address fields in contact editing are collapsed until needed. Account setup is grouped by messenger and exposes personal/web and bot paths explicitly. Settings separate tempo, background-queue behavior (honor service waits vs pause, skip inaccessible recipients vs pause, web preparation timeout), number formatting, WhatsApp service, stop-list and backup controls; advanced groups expand only on demand. Queue status and message inspection are separate blocks.

## Truth and safety

Never call a click or API acceptance delivery. Web account support remains visibly experimental and requires login/compatibility testing. Keep permission state distinct from service connection state. No fake success metrics or invented recipient counts. Existing tokens, contacts, snapshots and explicit-send confirmation behavior remain unchanged.

## Verification

Root agent owns emulator installation and screenshots. This source change does not operate the user's phone.

Night mode also uses a native dark Android theme for dialogs and controls; web login pages keep their own messenger styling.

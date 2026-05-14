import telebot
import random
import time
import threading

API_TOKEN = "8649962780:AAElaaF0sjMylFAWJ_e_53Lo_h0CxJ0A_1E"
bot = telebot.TeleBot(API_TOKEN)

CODE_EXPIRE_SECONDS = 180

@bot.message_handler(commands=["recovery"])
def cmd_recovery(message):
    pin = str(random.randint(100000, 999999))
    msg = bot.reply_to(message, f"Code:{pin}")
    
    try:
        bot.delete_message(message.chat.id, message.message_id)
    except:
        pass
    
    def delete_message():
        time.sleep(CODE_EXPIRE_SECONDS)
        try:
            bot.delete_message(msg.chat.id, msg.id)
        except:
            pass
    
    threading.Thread(target=delete_message, daemon=True).start()

if __name__ == "__main__":
    print("Bot started...")
    bot.infinity_polling()

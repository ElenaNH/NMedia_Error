package ru.netology.nmedia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters  // пока не работает (понадобится, когда включим аннотацию)
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.PostEntity

// Объявляем AppDb - абстрактный класс
// Его имплементацию сгенерит библиотека ROOM сама
// Мы только должны указать в аннотации все Entity-классы (структуры всех таблиц)
//@Database(entities = [PostEntity::class], version = 1)

// Новый вариант аннотаций (при переходе к DependencyContainer)
@Database(entities = [PostEntity::class], version = 1, exportSchema = false)
//@TypeConverters(Converters::class)  // Это пока не работает: не создали класс ...nmedia.dao.Converters
abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao

}


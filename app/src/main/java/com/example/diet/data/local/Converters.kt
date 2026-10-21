package com.example.diet.data.local

import androidx.room.TypeConverter
import com.example.diet.domain.model.ActivityLevel
import com.example.diet.domain.model.DrinkType
import com.example.diet.domain.model.Gender
import com.example.diet.domain.model.MealType

class Converters {
    @TypeConverter fun genderToStr(v: Gender) = v.name
    @TypeConverter fun strToGender(v: String) = enumValueOf<Gender>(v)

    @TypeConverter fun activityToStr(v: ActivityLevel) = v.name
    @TypeConverter fun strToActivity(v: String) = enumValueOf<ActivityLevel>(v)

    @TypeConverter fun mealTypeToStr(v: MealType) = v.name
    @TypeConverter fun strToMealType(v: String) = enumValueOf<MealType>(v)

    @TypeConverter fun drinkTypeToStr(v: DrinkType) = v.name
    @TypeConverter fun strToDrinkType(v: String) = enumValueOf<DrinkType>(v)
}

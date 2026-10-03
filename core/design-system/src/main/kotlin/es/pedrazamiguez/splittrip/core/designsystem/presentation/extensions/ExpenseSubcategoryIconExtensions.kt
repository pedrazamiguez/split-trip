package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AirBalloon
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowsTransferUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Backpack
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Beer
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Brush
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingCarousel
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingCommunity
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingCottage
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingEstate
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingSkyscraper
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Car
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CarSuv
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChefHat
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleDotted
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Coffee
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cpu
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.DeviceGamepad2
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.DeviceSim
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.GasStation
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Gift
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.GlassCocktail
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Home2
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.MasksTheater
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.MedicalCross
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Microphone2
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Motorbike
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Mountain
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PaperBag
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ParkingCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Physiotherapist
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PicnicTable
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plane
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PlaneTilt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Pool
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptEuro
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShieldBolt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShieldHeart
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShieldLock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShieldPin
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shirt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShoppingBag
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.SoccerField
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Speedboat
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Tent
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.TipJarEuro
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Train
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.TruckDelivery
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Van
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.WashMachine
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

private val subcategoryToIconMap: Map<ExpenseSubcategory, ImageVector> = mapOf(
    ExpenseSubcategory.INTERNATIONAL_FLIGHT to TablerIcons.Outline.PlaneTilt,
    ExpenseSubcategory.DOMESTIC_FLIGHT to TablerIcons.Outline.Plane,
    ExpenseSubcategory.TRAIN to TablerIcons.Outline.Train,
    ExpenseSubcategory.BUS to TablerIcons.Outline.Bus,
    ExpenseSubcategory.TAXI_RIDESHARE to TablerIcons.Outline.Car,
    ExpenseSubcategory.PRIVATE_TRANSFER to TablerIcons.Outline.Van,
    ExpenseSubcategory.CAR_RENTAL to TablerIcons.Outline.CarSuv,
    ExpenseSubcategory.MOTORBIKE_RENTAL to TablerIcons.Outline.Motorbike,
    ExpenseSubcategory.FUEL to TablerIcons.Outline.GasStation,
    ExpenseSubcategory.TOLLS_PARKING to TablerIcons.Outline.ParkingCircle,
    ExpenseSubcategory.FERRY_BOAT to TablerIcons.Outline.Speedboat,
    ExpenseSubcategory.PUBLIC_TRANSIT to TablerIcons.Outline.ArrowsTransferUp,
    ExpenseSubcategory.RESTAURANT to TablerIcons.Outline.ChefHat,
    ExpenseSubcategory.STREET_FOOD to TablerIcons.Outline.PicnicTable,
    ExpenseSubcategory.GROCERIES_SUPERMARKET to TablerIcons.Outline.ShoppingBag,
    ExpenseSubcategory.CAFE_BREAKFAST to TablerIcons.Outline.Coffee,
    ExpenseSubcategory.BAR_DRINKS to TablerIcons.Outline.Beer,
    ExpenseSubcategory.DELIVERY to TablerIcons.Outline.TruckDelivery,
    ExpenseSubcategory.HOTEL to TablerIcons.Outline.BuildingCommunity,
    ExpenseSubcategory.HOSTEL to TablerIcons.Outline.Home2,
    ExpenseSubcategory.VACATION_RENTAL to TablerIcons.Outline.BuildingEstate,
    ExpenseSubcategory.RESORT to TablerIcons.Outline.BuildingSkyscraper,
    ExpenseSubcategory.CAMPING to TablerIcons.Outline.Tent,
    ExpenseSubcategory.HOMESTAY to TablerIcons.Outline.BuildingCottage,
    ExpenseSubcategory.MUSEUM_CULTURE to TablerIcons.Outline.Brush,
    ExpenseSubcategory.TOUR_EXCURSION to TablerIcons.Outline.Backpack,
    ExpenseSubcategory.NATURE_PARK to TablerIcons.Outline.Mountain,
    ExpenseSubcategory.WATER_SPORTS to TablerIcons.Outline.Pool,
    ExpenseSubcategory.ADVENTURE_SPORTS to TablerIcons.Outline.AirBalloon,
    ExpenseSubcategory.SPA_WELLNESS to TablerIcons.Outline.Physiotherapist,
    ExpenseSubcategory.TICKETS_ATTRACTIONS to TablerIcons.Outline.BuildingCarousel,
    ExpenseSubcategory.CONCERT_FESTIVAL to TablerIcons.Outline.Microphone2,
    ExpenseSubcategory.NIGHTLIFE_CLUB to TablerIcons.Outline.GlassCocktail,
    ExpenseSubcategory.CINEMA_THEATER to TablerIcons.Outline.MasksTheater,
    ExpenseSubcategory.GAMES_ARCADE to TablerIcons.Outline.DeviceGamepad2,
    ExpenseSubcategory.SPORTS_EVENT to TablerIcons.Outline.SoccerField,
    ExpenseSubcategory.CLOTHING to TablerIcons.Outline.Shirt,
    ExpenseSubcategory.SOUVENIRS_GIFTS to TablerIcons.Outline.Gift,
    ExpenseSubcategory.ELECTRONICS to TablerIcons.Outline.Cpu,
    ExpenseSubcategory.PHARMACY_HEALTH to TablerIcons.Outline.MedicalCross,
    ExpenseSubcategory.CONVENIENCE_STORE to TablerIcons.Outline.PaperBag,
    ExpenseSubcategory.TRAVEL_INSURANCE to TablerIcons.Outline.ShieldPin,
    ExpenseSubcategory.MEDICAL_HEALTH to TablerIcons.Outline.ShieldHeart,
    ExpenseSubcategory.VEHICLE_INSURANCE to TablerIcons.Outline.ShieldBolt,
    ExpenseSubcategory.EQUIPMENT_INSURANCE to TablerIcons.Outline.ShieldLock,
    ExpenseSubcategory.TIPS_GRATUITIES to TablerIcons.Outline.TipJarEuro,
    ExpenseSubcategory.SIM_COMMUNICATIONS to TablerIcons.Outline.DeviceSim,
    ExpenseSubcategory.LAUNDRY to TablerIcons.Outline.WashMachine,
    ExpenseSubcategory.FEES_SURCHARGES to TablerIcons.Outline.ReceiptEuro,
    ExpenseSubcategory.UNSPECIFIED to TablerIcons.Outline.CircleDotted
)

fun ExpenseSubcategory.toIconVector(): ImageVector =
    subcategoryToIconMap[this] ?: TablerIcons.Outline.CircleDotted

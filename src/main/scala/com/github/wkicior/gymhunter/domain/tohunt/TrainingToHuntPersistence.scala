package com.github.wkicior.gymhunter.domain.tohunt

import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntAggregate.TrainingToHuntEvent

object TrainingToHuntPersistence {
  final case class GetAllTrainingsToHunt()
  final case class GetTrainingToHuntAggregate(id: TrainingToHuntId)
  final case class GetTrainingToHunt(id: TrainingToHuntId)
  final case class StoreEvents(id: TrainingToHuntId, events: List[TrainingToHuntEvent])
}



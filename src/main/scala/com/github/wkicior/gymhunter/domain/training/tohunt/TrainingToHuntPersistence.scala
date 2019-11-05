package com.github.wkicior.gymhunter.domain.training.tohunt

import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntAggregate.TrainingToHuntEvent

object TrainingToHuntPersistence {
  final case class GetAllTrainingsToHunt()
  final case class GetTrainingToHuntAggregate(id: TrainingToHuntId)
  final case class GetTrainingToHunt(id: TrainingToHuntId)
  final case class StoreEvents(id: TrainingToHuntId, events: List[TrainingToHuntEvent])

  type OptionalTrainingToHunt[+A] = Either[TrainingToHuntNotFound, A]
  final case class TrainingToHuntNotFound(id: TrainingToHuntId) extends RuntimeException(s"Training to hunt not found with id $id")
}

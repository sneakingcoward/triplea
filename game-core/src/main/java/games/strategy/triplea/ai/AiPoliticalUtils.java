package games.strategy.triplea.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.triplea.java.collections.CollectionUtils;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;

/**
 * Basic utility methods to handle basic AI stuff for Politics this AI always tries to get from Neutral to War with
 * state if it is free with everyone this AI will not go through a different Neutral state to reach a War state
 * (i.e. go from NAP to Peace to War).
 */
public final class AiPoliticalUtils {
  private AiPoliticalUtils() {}

  public static List<PoliticalActionAttachment> getPoliticalActionsTowardsWar(final PlayerId id,
      final Map<ICondition, Boolean> testedConditions, final GameData data) {
    final List<PoliticalActionAttachment> acceptableActions = new ArrayList<>();
    for (final PoliticalActionAttachment nextAction : PoliticalActionAttachment.getValidActions(id, testedConditions,
        data)) {
      if (wantToPerFormActionTowardsWar(nextAction, id, data)) {
        acceptableActions.add(nextAction);
      }
    }
    return acceptableActions;
  }

  /**
   * Returns the non-war political actions considered acceptable for the specified player under the specified
   * conditions.
   */
  public static List<PoliticalActionAttachment> getPoliticalActionsOther(final PlayerId id,
      final Map<ICondition, Boolean> testedConditions, final GameData data) {
    final List<PoliticalActionAttachment> warActions = getPoliticalActionsTowardsWar(id, testedConditions, data);
    final Collection<PoliticalActionAttachment> validActions =
        PoliticalActionAttachment.getValidActions(id, testedConditions, data);
    validActions.removeAll(warActions);
    final List<PoliticalActionAttachment> acceptableActions = new ArrayList<>();
    for (final PoliticalActionAttachment nextAction : validActions) {
      if (warActions.contains(nextAction)) {
        continue;
      }
      if (goesTowardsWar(nextAction, id, data) && Math.random() < .5) {
        continue;
      }
      if (awayFromAlly(nextAction, id, data) && Math.random() < .9) {
        continue;
      }
      if (isFree(nextAction)) {
        acceptableActions.add(nextAction);
      } else if (CollectionUtils.countMatches(validActions, politicalActionHasNoResourceCost()) > 1) {
        if (Math.random() < .9 && isAcceptableCost(nextAction, id, data)) {
          acceptableActions.add(nextAction);
        }
      } else {
        if (Math.random() < .4 && isAcceptableCost(nextAction, id, data)) {
          acceptableActions.add(nextAction);
        }
      }
    }
    return acceptableActions;
  }

  private static Predicate<PoliticalActionAttachment> politicalActionHasNoResourceCost() {
    return paa -> paa.getCostResources().isEmpty();
  }

  private static boolean wantToPerFormActionTowardsWar(final PoliticalActionAttachment nextAction, final PlayerId id,
      final GameData data) {
    return isFree(nextAction) && goesTowardsWar(nextAction, id, data);
  }

  // this code has a rare risk of circular loop actions.. depending on the map designer
  // only switches from a Neutral to an War state... won't go through in-between neutral states
  // TODO have another look at this part.
  private static boolean goesTowardsWar(final PoliticalActionAttachment nextAction, final PlayerId p0,
      final GameData data) {
    for (final PoliticalActionAttachment.RelationshipChange relationshipChange : nextAction.getRelationshipChanges()) {
      final PlayerId p1 = relationshipChange.player1;
      final PlayerId p2 = relationshipChange.player2;
      // only continue if p1 or p2 is the AI
      if (p0.equals(p1) || p0.equals(p2)) {
        final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
        final RelationshipType newType = relationshipChange.relationshipType;
        if (currentType.getRelationshipTypeAttachment().isNeutral()
            && newType.getRelationshipTypeAttachment().isWar()) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean awayFromAlly(final PoliticalActionAttachment nextAction, final PlayerId p0,
      final GameData data) {
    for (final PoliticalActionAttachment.RelationshipChange relationshipChange : nextAction.getRelationshipChanges()) {
      final PlayerId p1 = relationshipChange.player1;
      final PlayerId p2 = relationshipChange.player2;
      // only continue if p1 or p2 is the AI
      if (p0.equals(p1) || p0.equals(p2)) {
        final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
        final RelationshipType newType = relationshipChange.relationshipType;
        if (currentType.getRelationshipTypeAttachment().isAllied()
            && (newType.getRelationshipTypeAttachment().isNeutral()
                || newType.getRelationshipTypeAttachment().isWar())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isFree(final PoliticalActionAttachment nextAction) {
    return nextAction.getCostResources().isEmpty();
  }

  private static boolean isAcceptableCost(final PoliticalActionAttachment nextAction, final PlayerId player,
      final GameData data) {
    // if we have 21 or more PUs and the cost of the action is l0% or less of our total money, then it is an acceptable
    // price.
    final float production = AbstractEndTurnDelegate.getProduction(data.getMap().getTerritoriesOwnedBy(player), data);
    final Resource r = data.getResourceList().getResource(Constants.PUS);
    return production >= 21 && nextAction.getCostResources().getInt(r) <= (production / 10);
  }
}

using System;
using UnityEngine;

namespace FundingFloor {
  [KSPScenario(ScenarioCreationOptions.AddToNewCareerGames | ScenarioCreationOptions.AddToExistingCareerGames,
    GameScenes.SPACECENTER, GameScenes.EDITOR, GameScenes.FLIGHT, GameScenes.TRACKSTATION)]
  public class FundingFloorScenario : ScenarioModule {
    Settings cfg;

    static void Log(string message) {
      Debug.Log($"[FundingFloor] {message}");
    }

    public void Start() {
      Log("Starting up Funding Floor...");
      cfg = HighLogic.CurrentGame.Parameters.CustomParams<Settings>();

      // GameEvents.onGameNewStart.Add(OnGameNewStart);
      // GameEvents.onGameStatePostLoad.Add(OnGameStatePostLoad);
      GameEvents.OnGameSettingsApplied.Add(OnGameSettingsApplied);

      GameEvents.OnFundsChanged.Add(OnFundsChanged);
      GameEvents.OnReputationChanged.Add(OnReputationChanged);
      GameEvents.OnScienceChanged.Add(OnScienceChanged);
      GameEvents.OnTechnologyResearched.Add(OnTechnologyResearched);

      CalcBudget();
      ClampFunds();

      // GameEvents.OnKSCFacilityUpgraded
      // GameEvents.OnProgressComplete
      Log("Funding Floor started.");
    }

    public void OnDisable() {
      Log("Shutting down Funding Floor...");

      // GameEvents.onGameNewStart.Remove(OnGameNewStart);
      // GameEvents.onGameStatePostLoad.Remove(OnGameStatePostLoad);
      GameEvents.OnGameSettingsApplied.Remove(OnGameSettingsApplied);

      GameEvents.OnFundsChanged.Remove(OnFundsChanged);
      GameEvents.OnReputationChanged.Remove(OnReputationChanged);
      GameEvents.OnScienceChanged.Remove(OnScienceChanged);
      GameEvents.OnTechnologyResearched.Remove(OnTechnologyResearched);

      Log("Funding Floor shut down.");
    }

    int budget;

    void UpdateFunding() {
      CalcBudget(); ClampFunds();
    }

    // Budget is a linear interpolation between (MinFunding + science bonus)
    // and MaxFunding. This also means that the science bonus counts for more
    // when your reputation is lower.
    void CalcBudget() {
      double actual_min = Math.Min(
        cfg.MinFunding + science_spent * cfg.FundingPerScience,
        cfg.MaxFunding - 1000);
      double funds_per_rep = (cfg.MaxFunding - cfg.MinFunding)/1000.0;
      Log($"Calculating budget with min={cfg.MinFunding} sci={science_spent} amin={actual_min} max={cfg.MaxFunding} f/r={funds_per_rep}");
      budget = (int)(actual_min + Reputation.CurrentRep * funds_per_rep);
      Log($"Budget recalculated as {budget}");
    }

    void ClampFunds() {
      float rep = Reputation.CurrentRep;
      double funds = Funding.Instance.Funds;
      if (cfg.KeepExcessFunding ? (funds >= budget) : (funds == budget)) {
        return;
      }
      Log($"Setting player funds to {budget}");
      Funding.Instance.SetFunds(budget, TransactionReasons.None);
    }

    // public void OnGameNewStart() {
    //   Log($"Start: funds={Funding.Instance.Funds} rep={Reputation.CurrentRep})");
    //   UpdateFunding();
    // }

    // public void OnGameStatePostLoad(ConfigNode unused) {
    //   Log($"PostLoad: funds={Funding.Instance.Funds} rep={Reputation.CurrentRep})");
    //   UpdateFunding();
    // }

    public void OnGameSettingsApplied() {
      Log($"SettingsApplied: funds={Funding.Instance.Funds} rep={Reputation.CurrentRep})");
      cfg = HighLogic.CurrentGame.Parameters.CustomParams<Settings>();
      UpdateFunding();
    }

    public void OnFundsChanged(double val, TransactionReasons txns) {
      if (Funding.Instance == null) return;

      Log($"OnFundsChanged: funds={Funding.Instance.Funds} new={val}, why={txns})");
      UpdateFunding();
    }

    public void OnScienceChanged(float val, TransactionReasons txns) {
      if (ResearchAndDevelopment.Instance == null) return;

      Log($"OnScienceChanged: science={ResearchAndDevelopment.Instance.Science} new={val}, why={txns})");
    }

    public void OnReputationChanged(float val, TransactionReasons txns) {
      if (Reputation.Instance == null) return;

      Log($"OnReputationChanged: rep={Reputation.CurrentRep} new={val}, why={txns})");
      UpdateFunding();
    }

    [KSPField(isPersistant = true, guiActive = false)]
    int science_spent;

    public void OnTechnologyResearched(GameEvents.HostTargetAction<RDTech, RDTech.OperationResult> action) {
      if (action.target != RDTech.OperationResult.Successful) return;
      science_spent += action.host.scienceCost;
      Log($"OnTechResearched: cost={action.host.scienceCost} total={science_spent}");
      UpdateFunding();
    }
  }
}
